-- MobinSR compatibility shim for the common lua-cjson API.
-- This is Lua code backed by the bundled JSON4Lua module, not a native cjson .so.
-- It intentionally implements the common encode/decode/null/safe interface only.
local json = require("json")
local cjson = {}

cjson.null = json.null
cjson.encode = json.encode
cjson.decode = json.decode

function cjson.safe_encode(value)
  local ok, result = pcall(json.encode, value)
  if ok then return result end
  return nil, result
end

function cjson.safe_decode(value)
  local ok, result = pcall(json.decode, value)
  if ok then return result end
  return nil, result
end

function cjson.new()
  local instance = {}
  for k, v in pairs(cjson) do instance[k] = v end
  return instance
end

-- These configuration calls are accepted for source compatibility. JSON4Lua
-- controls its own formatting and does not implement cjson's native tunables.
function cjson.encode_sparse_array() return true end
function cjson.encode_invalid_numbers() return true end
function cjson.decode_invalid_numbers() return true end
function cjson.encode_keep_buffer() return true end
function cjson.encode_number_precision() return true end
function cjson.encode_max_depth() return true end
function cjson.decode_max_depth() return true end

return cjson
