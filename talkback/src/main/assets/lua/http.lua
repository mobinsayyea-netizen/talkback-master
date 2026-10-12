-----------------------------------------------------------------------------
-- MobinSR HTTP module
-- Replaces the LuaSocket implementation for this runtime: Jieshuo uses Java
-- networking and this project does not bundle LuaSocket/ltn12/mime.
-- API compatibility: get/post -> body,cookie,status,headers; download -> status,headers.
-----------------------------------------------------------------------------
local URL = luajava.bindClass("java.net.URL")
local File = luajava.bindClass("java.io.File")
local FileOutputStream = luajava.bindClass("java.io.FileOutputStream")
local ByteArrayOutputStream = luajava.bindClass("java.io.ByteArrayOutputStream")
local URLEncoder = luajava.bindClass("java.net.URLEncoder")
local _M = { TIMEOUT = 15000, USERAGENT = "MobinSR-Lua/1.0" }

local function headersOf(conn)
  local out = {}
  local fields = conn:getHeaderFields()
  if fields == nil then return out end
  local it = fields:keySet():iterator()
  while it:hasNext() do
    local key = it:next()
    if key ~= nil then
      local values = fields:get(key)
      if values ~= nil and values:size() > 0 then out[tostring(key):lower()] = tostring(values:get(0)) end
    end
  end
  return out
end

local function responseStream(conn, status)
  if status >= 400 then return conn:getErrorStream() end
  return conn:getInputStream()
end

local function readAll(stream)
  if stream == nil then return "" end
  local out = ByteArrayOutputStream()
  local buf = luajava.newArray("byte", 8192)
  local n = stream:read(buf)
  while n ~= -1 do
    if n > 0 then out:write(buf, 0, n) end
    n = stream:read(buf)
  end
  stream:close()
  return tostring(out:toString("UTF-8"))
end

local function encodeForm(data)
  if type(data) == "string" then return data end
  if type(data) ~= "table" then return "" end
  local out = {}
  for k, v in pairs(data) do
    out[#out + 1] = tostring(URLEncoder:encode(tostring(k), "UTF-8")) .. "=" .. tostring(URLEncoder:encode(tostring(v), "UTF-8"))
  end
  return table.concat(out, "&")
end

local function open(url, method, cookie, body, contentType)
  local conn = URL(tostring(url)):openConnection()
  conn:setConnectTimeout(_M.TIMEOUT)
  conn:setReadTimeout(_M.TIMEOUT)
  conn:setRequestMethod(method)
  conn:setRequestProperty("User-Agent", _M.USERAGENT)
  conn:setRequestProperty("Accept", "*/*")
  if cookie ~= nil and tostring(cookie) ~= "" then conn:setRequestProperty("Cookie", tostring(cookie)) end
  if body ~= nil then
    conn:setDoOutput(true)
    conn:setRequestProperty("Content-Type", contentType or "application/x-www-form-urlencoded; charset=UTF-8")
    local os = conn:getOutputStream()
    os:write(tostring(body):getBytes("UTF-8"))
    os:flush()
    os:close()
  end
  local status = conn:getResponseCode()
  local headers = headersOf(conn)
  local cookies = headers["set-cookie"] or cookie or ""
  return conn, status, headers, cookies
end

local function getImpl(url, cookie)
  local conn, status, headers, cookies = open(url, "GET", cookie)
  local ok, body = pcall(readAll, responseStream(conn, status))
  pcall(function() conn:disconnect() end)
  if not ok then return nil, cookies, status, headers, tostring(body) end
  return body, cookies, status, headers
end

function _M.get(url, cookie)
  local ok, a, b, c, d, e = pcall(getImpl, url, cookie)
  if not ok then return nil, cookie or "", nil, {}, tostring(a) end
  return a, b, c, d, e
end

local function postImpl(url, data, cookie)
  local conn, status, headers, cookies = open(url, "POST", cookie, encodeForm(data))
  local ok, body = pcall(readAll, responseStream(conn, status))
  pcall(function() conn:disconnect() end)
  if not ok then return nil, cookies, status, headers, tostring(body) end
  return body, cookies, status, headers
end

function _M.post(url, data, cookie)
  local ok, a, b, c, d, e = pcall(postImpl, url, data, cookie)
  if not ok then return nil, cookie or "", nil, {}, tostring(a) end
  return a, b, c, d, e
end

local function downloadImpl(url, path, cookie)
  local conn, status, headers = open(url, "GET", cookie)
  local stream = responseStream(conn, status)
  if status >= 400 or stream == nil then
    if stream ~= nil then stream:close() end
    conn:disconnect()
    return status, headers
  end
  local file = File(tostring(path))
  local parent = file:getParentFile()
  if parent ~= nil then parent:mkdirs() end
  local os = FileOutputStream(file)
  local buf = luajava.newArray("byte", 8192)
  local n = stream:read(buf)
  while n ~= -1 do
    if n > 0 then os:write(buf, 0, n) end
    n = stream:read(buf)
  end
  stream:close()
  os:flush()
  os:close()
  pcall(function() conn:disconnect() end)
  return status, headers
end

function _M.download(url, path, cookie)
  local ok, a, b, c, d = pcall(downloadImpl, url, path, cookie)
  if not ok then return nil, {}, tostring(a) end
  return a, b, c, d
end

-- Multipart upload compatible with AndroLua's http.upload(url, fields, files, cookie).
local function uploadImpl(url, fields, files, cookie)
  local boundary = "----MobinSR" .. tostring(os.time()) .. tostring(math.random(100000, 999999))
  local conn = URL(tostring(url)):openConnection()
  conn:setConnectTimeout(_M.TIMEOUT)
  conn:setReadTimeout(_M.TIMEOUT)
  conn:setRequestMethod("POST")
  conn:setDoOutput(true)
  conn:setRequestProperty("User-Agent", _M.USERAGENT)
  conn:setRequestProperty("Content-Type", "multipart/form-data; boundary=" .. boundary)
  if cookie ~= nil and tostring(cookie) ~= "" then conn:setRequestProperty("Cookie", tostring(cookie)) end
  local os = conn:getOutputStream()
  local function write(s) os:write(tostring(s):getBytes("UTF-8")) end
  if type(fields) == "table" then
    for k, v in pairs(fields) do
      write("--" .. boundary .. "\r\nContent-Disposition: form-data; name=\"" .. tostring(k) .. "\"\r\n\r\n" .. tostring(v) .. "\r\n")
    end
  end
  if type(files) == "table" then
    for k, v in pairs(files) do
      local path = tostring(v)
      local f = File(path)
      write("--" .. boundary .. "\r\nContent-Disposition: form-data; name=\"" .. tostring(k) .. "\"; filename=\"" .. f:getName() .. "\"\r\nContent-Type: application/octet-stream\r\n\r\n")
      local fis = luajava.bindClass("java.io.FileInputStream")(f)
      local buf = luajava.newArray("byte", 8192)
      local n = fis:read(buf)
      while n ~= -1 do
        if n > 0 then os:write(buf, 0, n) end
        n = fis:read(buf)
      end
      fis:close()
      write("\r\n")
    end
  end
  write("--" .. boundary .. "--\r\n")
  os:flush()
  os:close()
  local status = conn:getResponseCode()
  local headers = headersOf(conn)
  local cookies = headers["set-cookie"] or cookie or ""
  local stream = responseStream(conn, status)
  local ok, body = pcall(readAll, stream)
  pcall(function() conn:disconnect() end)
  if not ok then return nil, cookies, status, headers, tostring(body) end
  return body, cookies, status, headers
end

function _M.upload(url, fields, files, cookie)
  local ok, a, b, c, d, e = pcall(uploadImpl, url, fields, files, cookie)
  if not ok then return nil, cookie or "", nil, {}, tostring(a) end
  return a, b, c, d, e
end

return _M
