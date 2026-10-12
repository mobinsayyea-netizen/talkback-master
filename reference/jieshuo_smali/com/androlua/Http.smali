.class public Lcom/androlua/Http;
.super Ljava/lang/Object;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lcom/androlua/Http$HttpTask;
    }
.end annotation


# static fields
.field private static final boundary:Ljava/lang/String; = "----q1w2e3r4t5y6u7i8o9p0a1s2d3f4g5h6j7k8l9z0x1c2v3b4n5m6"

.field private static sHeader:Ljava/util/HashMap;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;"
        }
    .end annotation
.end field


# direct methods
.method static constructor <clinit>()V
    .locals 7

    const-string v0, "SSL"

    const/4 v1, 0x0

    const/4 v2, 0x1

    const/4 v3, 0x0

    :try_start_0
    invoke-static {v0}, Ljavax/net/ssl/SSLContext;->getInstance(Ljava/lang/String;)Ljavax/net/ssl/SSLContext;

    move-result-object v4

    new-array v5, v2, [Ljavax/net/ssl/TrustManager;

    new-instance v6, Lcom/androlua/Http$1;

    invoke-direct {v6}, Lcom/androlua/Http$1;-><init>()V

    aput-object v6, v5, v1

    new-instance v6, Ljava/security/SecureRandom;

    invoke-direct {v6}, Ljava/security/SecureRandom;-><init>()V

    invoke-virtual {v4, v3, v5, v6}, Ljavax/net/ssl/SSLContext;->init([Ljavax/net/ssl/KeyManager;[Ljavax/net/ssl/TrustManager;Ljava/security/SecureRandom;)V

    new-instance v5, Lcom/androlua/Http$2;

    invoke-direct {v5}, Lcom/androlua/Http$2;-><init>()V

    invoke-static {v5}, Ljavax/net/ssl/HttpsURLConnection;->setDefaultHostnameVerifier(Ljavax/net/ssl/HostnameVerifier;)V

    invoke-virtual {v4}, Ljavax/net/ssl/SSLContext;->getSocketFactory()Ljavax/net/ssl/SSLSocketFactory;

    move-result-object v4

    invoke-static {v4}, Ljavax/net/ssl/HttpsURLConnection;->setDefaultSSLSocketFactory(Ljavax/net/ssl/SSLSocketFactory;)V
    :try_end_0
    .catch Ljava/security/NoSuchAlgorithmException; {:try_start_0 .. :try_end_0} :catch_1
    .catch Ljava/security/KeyManagementException; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_1

    :catch_0
    move-exception v4

    goto :goto_0

    :catch_1
    move-exception v4

    :goto_0
    invoke-virtual {v4}, Ljava/lang/Throwable;->printStackTrace()V

    :goto_1
    :try_start_1
    invoke-static {v0}, Ljavax/net/ssl/SSLContext;->getInstance(Ljava/lang/String;)Ljavax/net/ssl/SSLContext;

    move-result-object v0

    new-array v2, v2, [Ljavax/net/ssl/TrustManager;

    new-instance v4, Lcom/androlua/Http$3;

    invoke-direct {v4}, Lcom/androlua/Http$3;-><init>()V

    aput-object v4, v2, v1

    new-instance v1, Ljava/security/SecureRandom;

    invoke-direct {v1}, Ljava/security/SecureRandom;-><init>()V

    invoke-virtual {v0, v3, v2, v1}, Ljavax/net/ssl/SSLContext;->init([Ljavax/net/ssl/KeyManager;[Ljavax/net/ssl/TrustManager;Ljava/security/SecureRandom;)V

    new-instance v1, Lcom/androlua/Http$4;

    invoke-direct {v1}, Lcom/androlua/Http$4;-><init>()V

    invoke-static {v1}, Ljavax/net/ssl/HttpsURLConnection;->setDefaultHostnameVerifier(Ljavax/net/ssl/HostnameVerifier;)V

    invoke-virtual {v0}, Ljavax/net/ssl/SSLContext;->getSocketFactory()Ljavax/net/ssl/SSLSocketFactory;

    move-result-object v0

    invoke-static {v0}, Ljavax/net/ssl/HttpsURLConnection;->setDefaultSSLSocketFactory(Ljavax/net/ssl/SSLSocketFactory;)V
    :try_end_1
    .catch Ljava/security/NoSuchAlgorithmException; {:try_start_1 .. :try_end_1} :catch_3
    .catch Ljava/security/KeyManagementException; {:try_start_1 .. :try_end_1} :catch_2

    goto :goto_3

    :catch_2
    move-exception v0

    goto :goto_2

    :catch_3
    move-exception v0

    :goto_2
    invoke-virtual {v0}, Ljava/lang/Throwable;->printStackTrace()V

    :goto_3
    return-void
.end method

.method public constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method static bridge synthetic a()Ljava/util/HashMap;
    .locals 1

    sget-object v0, Lcom/androlua/Http;->sHeader:Ljava/util/HashMap;

    return-object v0
.end method

.method public static delete(Ljava/lang/String;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 8

    new-instance v7, Lcom/androlua/Http$HttpTask;

    const-string v2, "DELETE"

    const/4 v3, 0x0

    const/4 v4, 0x0

    const/4 v5, 0x0

    move-object v0, v7

    move-object v1, p0

    move-object v6, p1

    invoke-direct/range {v0 .. v6}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    const/4 p0, 0x0

    new-array p0, p0, [Ljava/lang/Object;

    invoke-virtual {v7, p0}, Lcom/androlua/util/AsyncTaskX;->execute([Ljava/lang/Object;)Lcom/androlua/util/AsyncTaskX;

    return-object v7
.end method

.method public static delete(Ljava/lang/String;Ljava/lang/String;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 8

    const-string v0, "[\\w\\-\\.:]+"

    invoke-virtual {p1, v0}, Ljava/lang/String;->matches(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_0

    invoke-static {p1}, Ljava/nio/charset/Charset;->isSupported(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_0

    new-instance v0, Lcom/androlua/Http$HttpTask;

    const-string v3, "DELETE"

    const/4 v4, 0x0

    const/4 v6, 0x0

    move-object v1, v0

    move-object v2, p0

    move-object v5, p1

    move-object v7, p2

    invoke-direct/range {v1 .. v7}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    goto :goto_0

    :cond_0
    new-instance v0, Lcom/androlua/Http$HttpTask;

    const-string v3, "DELETE"

    const/4 v6, 0x0

    const/4 v7, 0x0

    move-object v1, v0

    move-object v2, p0

    move-object v4, p1

    move-object v5, v6

    move-object v6, v7

    move-object v7, p2

    invoke-direct/range {v1 .. v7}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    :goto_0
    const/4 v1, 0x0

    new-array v1, v1, [Ljava/lang/Object;

    invoke-virtual {v0, v1}, Lcom/androlua/util/AsyncTaskX;->execute([Ljava/lang/Object;)Lcom/androlua/util/AsyncTaskX;

    return-object v0
.end method

.method public static delete(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 8

    new-instance v7, Lcom/androlua/Http$HttpTask;

    const-string v2, "DELETE"

    const/4 v5, 0x0

    move-object v0, v7

    move-object v1, p0

    move-object v3, p1

    move-object v4, p2

    move-object v6, p3

    invoke-direct/range {v0 .. v6}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    const/4 p0, 0x0

    new-array p0, p0, [Ljava/lang/Object;

    invoke-virtual {v7, p0}, Lcom/androlua/util/AsyncTaskX;->execute([Ljava/lang/Object;)Lcom/androlua/util/AsyncTaskX;

    return-object v7
.end method

.method public static delete(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 8
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Lcom/luajava/LuaObject;",
            ")",
            "Lcom/androlua/Http$HttpTask;"
        }
    .end annotation

    new-instance v7, Lcom/androlua/Http$HttpTask;

    const-string v2, "DELETE"

    move-object v0, v7

    move-object v1, p0

    move-object v3, p1

    move-object v4, p2

    move-object v5, p3

    move-object v6, p4

    invoke-direct/range {v0 .. v6}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    const/4 p0, 0x0

    new-array p0, p0, [Ljava/lang/Object;

    invoke-virtual {v7, p0}, Lcom/androlua/util/AsyncTaskX;->execute([Ljava/lang/Object;)Lcom/androlua/util/AsyncTaskX;

    return-object v7
.end method

.method public static delete(Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 12
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Lcom/luajava/LuaObject;",
            ")",
            "Lcom/androlua/Http$HttpTask;"
        }
    .end annotation

    const-string v0, "[\\w\\-\\.:]+"

    invoke-virtual {p1, v0}, Ljava/lang/String;->matches(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_0

    invoke-static {p1}, Ljava/nio/charset/Charset;->isSupported(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_0

    new-instance v0, Lcom/androlua/Http$HttpTask;

    const-string v3, "DELETE"

    const/4 v4, 0x0

    move-object v1, v0

    move-object v2, p0

    move-object v5, p1

    move-object v6, p2

    move-object v7, p3

    invoke-direct/range {v1 .. v7}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    goto :goto_0

    :cond_0
    new-instance v0, Lcom/androlua/Http$HttpTask;

    const-string v7, "DELETE"

    const/4 v9, 0x0

    move-object v5, v0

    move-object v6, p0

    move-object v8, p1

    move-object v10, p2

    move-object v11, p3

    invoke-direct/range {v5 .. v11}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    :goto_0
    const/4 p0, 0x0

    new-array p0, p0, [Ljava/lang/Object;

    invoke-virtual {v0, p0}, Lcom/androlua/util/AsyncTaskX;->execute([Ljava/lang/Object;)Lcom/androlua/util/AsyncTaskX;

    return-object v0
.end method

.method public static delete(Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 8
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/lang/String;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Lcom/luajava/LuaObject;",
            ")",
            "Lcom/androlua/Http$HttpTask;"
        }
    .end annotation

    new-instance v7, Lcom/androlua/Http$HttpTask;

    const-string v2, "DELETE"

    const/4 v3, 0x0

    const/4 v4, 0x0

    move-object v0, v7

    move-object v1, p0

    move-object v5, p1

    move-object v6, p2

    invoke-direct/range {v0 .. v6}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    const/4 p0, 0x0

    new-array p0, p0, [Ljava/lang/Object;

    invoke-virtual {v7, p0}, Lcom/androlua/util/AsyncTaskX;->execute([Ljava/lang/Object;)Lcom/androlua/util/AsyncTaskX;

    return-object v7
.end method

.method public static download(Ljava/lang/String;Ljava/lang/String;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 8

    new-instance v7, Lcom/androlua/Http$HttpTask;

    const-string v2, "GET"

    const/4 v3, 0x0

    const/4 v4, 0x0

    const/4 v5, 0x0

    move-object v0, v7

    move-object v1, p0

    move-object v6, p2

    invoke-direct/range {v0 .. v6}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    const/4 p0, 0x1

    new-array p0, p0, [Ljava/lang/Object;

    const/4 p2, 0x0

    aput-object p1, p0, p2

    invoke-virtual {v7, p0}, Lcom/androlua/util/AsyncTaskX;->execute([Ljava/lang/Object;)Lcom/androlua/util/AsyncTaskX;

    return-object v7
.end method

.method public static download(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 8

    new-instance v7, Lcom/androlua/Http$HttpTask;

    const-string v2, "GET"

    const/4 v4, 0x0

    const/4 v5, 0x0

    move-object v0, v7

    move-object v1, p0

    move-object v3, p2

    move-object v6, p3

    invoke-direct/range {v0 .. v6}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    const/4 p0, 0x1

    new-array p0, p0, [Ljava/lang/Object;

    const/4 p2, 0x0

    aput-object p1, p0, p2

    invoke-virtual {v7, p0}, Lcom/androlua/util/AsyncTaskX;->execute([Ljava/lang/Object;)Lcom/androlua/util/AsyncTaskX;

    return-object v7
.end method

.method public static download(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 8
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Lcom/luajava/LuaObject;",
            ")",
            "Lcom/androlua/Http$HttpTask;"
        }
    .end annotation

    new-instance v7, Lcom/androlua/Http$HttpTask;

    const-string v2, "GET"

    const/4 v4, 0x0

    move-object v0, v7

    move-object v1, p0

    move-object v3, p2

    move-object v5, p3

    move-object v6, p4

    invoke-direct/range {v0 .. v6}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    const/4 p0, 0x1

    new-array p0, p0, [Ljava/lang/Object;

    const/4 p2, 0x0

    aput-object p1, p0, p2

    invoke-virtual {v7, p0}, Lcom/androlua/util/AsyncTaskX;->execute([Ljava/lang/Object;)Lcom/androlua/util/AsyncTaskX;

    return-object v7
.end method

.method public static download(Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 8
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Lcom/luajava/LuaObject;",
            ")",
            "Lcom/androlua/Http$HttpTask;"
        }
    .end annotation

    new-instance v7, Lcom/androlua/Http$HttpTask;

    const-string v2, "GET"

    const/4 v3, 0x0

    const/4 v4, 0x0

    move-object v0, v7

    move-object v1, p0

    move-object v5, p2

    move-object v6, p3

    invoke-direct/range {v0 .. v6}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    const/4 p0, 0x1

    new-array p0, p0, [Ljava/lang/Object;

    const/4 p2, 0x0

    aput-object p1, p0, p2

    invoke-virtual {v7, p0}, Lcom/androlua/util/AsyncTaskX;->execute([Ljava/lang/Object;)Lcom/androlua/util/AsyncTaskX;

    return-object v7
.end method

.method private static formatMap(Ljava/util/HashMap;)Ljava/lang/String;
    .locals 4
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;)",
            "Ljava/lang/String;"
        }
    .end annotation

    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {p0}, Ljava/util/HashMap;->entrySet()Ljava/util/Set;

    move-result-object v1

    invoke-interface {v1}, Ljava/util/Set;->iterator()Ljava/util/Iterator;

    move-result-object v1

    :goto_0
    invoke-interface {v1}, Ljava/util/Iterator;->hasNext()Z

    move-result v2

    if-eqz v2, :cond_0

    invoke-interface {v1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Ljava/util/Map$Entry;

    invoke-interface {v2}, Ljava/util/Map$Entry;->getKey()Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Ljava/lang/String;

    invoke-virtual {v0, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v3, "="

    invoke-virtual {v0, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-interface {v2}, Ljava/util/Map$Entry;->getValue()Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Ljava/lang/String;

    invoke-virtual {v0, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v2, "&"

    invoke-virtual {v0, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    goto :goto_0

    :cond_0
    invoke-virtual {p0}, Ljava/util/HashMap;->isEmpty()Z

    move-result p0

    if-nez p0, :cond_1

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->length()I

    move-result p0

    add-int/lit8 p0, p0, -0x1

    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->setLength(I)V

    :cond_1
    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    return-object p0
.end method

.method private static formatMultiDate(Ljava/util/HashMap;Ljava/util/HashMap;Ljava/lang/String;)[B
    .locals 9
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Ljava/lang/String;",
            ")[B"
        }
    .end annotation

    if-nez p2, :cond_0

    const-string p2, "UTF-8"

    :cond_0
    new-instance v0, Ljava/io/ByteArrayOutputStream;

    invoke-direct {v0}, Ljava/io/ByteArrayOutputStream;-><init>()V

    invoke-virtual {p0}, Ljava/util/HashMap;->entrySet()Ljava/util/Set;

    move-result-object p0

    invoke-interface {p0}, Ljava/util/Set;->iterator()Ljava/util/Iterator;

    move-result-object p0

    :goto_0
    invoke-interface {p0}, Ljava/util/Iterator;->hasNext()Z

    move-result v1

    const/4 v2, 0x2

    const/4 v3, 0x3

    const-string v4, "----q1w2e3r4t5y6u7i8o9p0a1s2d3f4g5h6j7k8l9z0x1c2v3b4n5m6"

    const/4 v5, 0x0

    const/4 v6, 0x1

    if-eqz v1, :cond_1

    invoke-interface {p0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Ljava/util/Map$Entry;

    :try_start_0
    const-string v7, "--%s\r\nContent-Disposition:form-data;name=\"%s\"\r\n\r\n%s\r\n"

    new-array v3, v3, [Ljava/lang/Object;

    aput-object v4, v3, v5

    invoke-interface {v1}, Ljava/util/Map$Entry;->getKey()Ljava/lang/Object;

    move-result-object v4

    aput-object v4, v3, v6

    invoke-interface {v1}, Ljava/util/Map$Entry;->getValue()Ljava/lang/Object;

    move-result-object v1

    aput-object v1, v3, v2

    invoke-static {v7, v3}, Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v1, p2}, Ljava/lang/String;->getBytes(Ljava/lang/String;)[B

    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/io/OutputStream;->write([B)V
    :try_end_0
    .catch Ljava/io/IOException; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_0

    :catch_0
    move-exception v1

    invoke-virtual {v1}, Ljava/lang/Throwable;->printStackTrace()V

    goto :goto_0

    :cond_1
    invoke-virtual {p1}, Ljava/util/HashMap;->entrySet()Ljava/util/Set;

    move-result-object p0

    invoke-interface {p0}, Ljava/util/Set;->iterator()Ljava/util/Iterator;

    move-result-object p0

    :goto_1
    invoke-interface {p0}, Ljava/util/Iterator;->hasNext()Z

    move-result p1

    if-eqz p1, :cond_2

    invoke-interface {p0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Ljava/util/Map$Entry;

    :try_start_1
    const-string v1, "--%s\r\nContent-Disposition:form-data;name=\"%s\";filename=\"%s\"\r\nContent-Type:application/octet-stream\r\n\r\n"

    new-array v7, v3, [Ljava/lang/Object;

    aput-object v4, v7, v5

    invoke-interface {p1}, Ljava/util/Map$Entry;->getKey()Ljava/lang/Object;

    move-result-object v8

    aput-object v8, v7, v6

    invoke-interface {p1}, Ljava/util/Map$Entry;->getValue()Ljava/lang/Object;

    move-result-object v8

    aput-object v8, v7, v2

    invoke-static {v1, v7}, Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v1, p2}, Ljava/lang/String;->getBytes(Ljava/lang/String;)[B

    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/io/OutputStream;->write([B)V

    new-instance v1, Ljava/io/FileInputStream;

    invoke-interface {p1}, Ljava/util/Map$Entry;->getValue()Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Ljava/lang/String;

    invoke-direct {v1, p1}, Ljava/io/FileInputStream;-><init>(Ljava/lang/String;)V

    invoke-static {v1}, Lcom/androlua/LuaUtil;->readAll(Ljava/io/InputStream;)[B

    move-result-object p1

    invoke-virtual {v0, p1}, Ljava/io/OutputStream;->write([B)V

    const-string p1, "\r\n"

    invoke-virtual {p1, p2}, Ljava/lang/String;->getBytes(Ljava/lang/String;)[B

    move-result-object p1

    invoke-virtual {v0, p1}, Ljava/io/OutputStream;->write([B)V
    :try_end_1
    .catch Ljava/io/IOException; {:try_start_1 .. :try_end_1} :catch_1

    goto :goto_1

    :catch_1
    move-exception p1

    invoke-virtual {p1}, Ljava/lang/Throwable;->printStackTrace()V

    goto :goto_1

    :cond_2
    :try_start_2
    const-string p0, "--%s--\r\n"

    new-array p1, v6, [Ljava/lang/Object;

    aput-object v4, p1, v5

    invoke-static {p0, p1}, Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;

    move-result-object p0

    invoke-virtual {p0, p2}, Ljava/lang/String;->getBytes(Ljava/lang/String;)[B

    move-result-object p0

    invoke-virtual {v0, p0}, Ljava/io/OutputStream;->write([B)V
    :try_end_2
    .catch Ljava/io/IOException; {:try_start_2 .. :try_end_2} :catch_2

    goto :goto_2

    :catch_2
    move-exception p0

    invoke-virtual {p0}, Ljava/lang/Throwable;->printStackTrace()V

    :goto_2
    invoke-virtual {v0}, Ljava/io/ByteArrayOutputStream;->toByteArray()[B

    move-result-object p0

    return-object p0
.end method

.method public static get(Ljava/lang/String;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 8

    new-instance v7, Lcom/androlua/Http$HttpTask;

    const-string v2, "GET"

    const/4 v3, 0x0

    const/4 v4, 0x0

    const/4 v5, 0x0

    move-object v0, v7

    move-object v1, p0

    move-object v6, p1

    invoke-direct/range {v0 .. v6}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    const/4 p0, 0x0

    new-array p0, p0, [Ljava/lang/Object;

    invoke-virtual {v7, p0}, Lcom/androlua/util/AsyncTaskX;->execute([Ljava/lang/Object;)Lcom/androlua/util/AsyncTaskX;

    return-object v7
.end method

.method public static get(Ljava/lang/String;Ljava/lang/String;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 8

    const-string v0, "[\\w\\-\\.:]+"

    invoke-virtual {p1, v0}, Ljava/lang/String;->matches(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_0

    invoke-static {p1}, Ljava/nio/charset/Charset;->isSupported(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_0

    new-instance v0, Lcom/androlua/Http$HttpTask;

    const-string v3, "GET"

    const/4 v4, 0x0

    const/4 v6, 0x0

    move-object v1, v0

    move-object v2, p0

    move-object v5, p1

    move-object v7, p2

    invoke-direct/range {v1 .. v7}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    goto :goto_0

    :cond_0
    new-instance v0, Lcom/androlua/Http$HttpTask;

    const-string v3, "GET"

    const/4 v6, 0x0

    const/4 v7, 0x0

    move-object v1, v0

    move-object v2, p0

    move-object v4, p1

    move-object v5, v6

    move-object v6, v7

    move-object v7, p2

    invoke-direct/range {v1 .. v7}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    :goto_0
    const/4 v1, 0x0

    new-array v1, v1, [Ljava/lang/Object;

    invoke-virtual {v0, v1}, Lcom/androlua/util/AsyncTaskX;->execute([Ljava/lang/Object;)Lcom/androlua/util/AsyncTaskX;

    return-object v0
.end method

.method public static get(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 8

    new-instance v7, Lcom/androlua/Http$HttpTask;

    const-string v2, "GET"

    const/4 v5, 0x0

    move-object v0, v7

    move-object v1, p0

    move-object v3, p1

    move-object v4, p2

    move-object v6, p3

    invoke-direct/range {v0 .. v6}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    const/4 p0, 0x0

    new-array p0, p0, [Ljava/lang/Object;

    invoke-virtual {v7, p0}, Lcom/androlua/util/AsyncTaskX;->execute([Ljava/lang/Object;)Lcom/androlua/util/AsyncTaskX;

    return-object v7
.end method

.method public static get(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 8
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Lcom/luajava/LuaObject;",
            ")",
            "Lcom/androlua/Http$HttpTask;"
        }
    .end annotation

    new-instance v7, Lcom/androlua/Http$HttpTask;

    const-string v2, "GET"

    move-object v0, v7

    move-object v1, p0

    move-object v3, p1

    move-object v4, p2

    move-object v5, p3

    move-object v6, p4

    invoke-direct/range {v0 .. v6}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    const/4 p0, 0x0

    new-array p0, p0, [Ljava/lang/Object;

    invoke-virtual {v7, p0}, Lcom/androlua/util/AsyncTaskX;->execute([Ljava/lang/Object;)Lcom/androlua/util/AsyncTaskX;

    return-object v7
.end method

.method public static get(Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 12
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Lcom/luajava/LuaObject;",
            ")",
            "Lcom/androlua/Http$HttpTask;"
        }
    .end annotation

    const-string v0, "[\\w\\-\\.:]+"

    invoke-virtual {p1, v0}, Ljava/lang/String;->matches(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_0

    invoke-static {p1}, Ljava/nio/charset/Charset;->isSupported(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_0

    new-instance v0, Lcom/androlua/Http$HttpTask;

    const-string v3, "GET"

    const/4 v4, 0x0

    move-object v1, v0

    move-object v2, p0

    move-object v5, p1

    move-object v6, p2

    move-object v7, p3

    invoke-direct/range {v1 .. v7}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    goto :goto_0

    :cond_0
    new-instance v0, Lcom/androlua/Http$HttpTask;

    const-string v7, "GET"

    const/4 v9, 0x0

    move-object v5, v0

    move-object v6, p0

    move-object v8, p1

    move-object v10, p2

    move-object v11, p3

    invoke-direct/range {v5 .. v11}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    :goto_0
    const/4 p0, 0x0

    new-array p0, p0, [Ljava/lang/Object;

    invoke-virtual {v0, p0}, Lcom/androlua/util/AsyncTaskX;->execute([Ljava/lang/Object;)Lcom/androlua/util/AsyncTaskX;

    return-object v0
.end method

.method public static get(Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 8
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/lang/String;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Lcom/luajava/LuaObject;",
            ")",
            "Lcom/androlua/Http$HttpTask;"
        }
    .end annotation

    new-instance v7, Lcom/androlua/Http$HttpTask;

    const-string v2, "GET"

    const/4 v3, 0x0

    const/4 v4, 0x0

    move-object v0, v7

    move-object v1, p0

    move-object v5, p1

    move-object v6, p2

    invoke-direct/range {v0 .. v6}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    const/4 p0, 0x0

    new-array p0, p0, [Ljava/lang/Object;

    invoke-virtual {v7, p0}, Lcom/androlua/util/AsyncTaskX;->execute([Ljava/lang/Object;)Lcom/androlua/util/AsyncTaskX;

    return-object v7
.end method

.method public static getHeader()Ljava/util/HashMap;
    .locals 1
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;"
        }
    .end annotation

    sget-object v0, Lcom/androlua/Http;->sHeader:Ljava/util/HashMap;

    return-object v0
.end method

.method public static post(Ljava/lang/String;Ljava/lang/String;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 8

    new-instance v7, Lcom/androlua/Http$HttpTask;

    const-string v2, "POST"

    const/4 v3, 0x0

    const/4 v4, 0x0

    const/4 v5, 0x0

    move-object v0, v7

    move-object v1, p0

    move-object v6, p2

    invoke-direct/range {v0 .. v6}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    const/4 p0, 0x1

    new-array p0, p0, [Ljava/lang/Object;

    const/4 p2, 0x0

    aput-object p1, p0, p2

    invoke-virtual {v7, p0}, Lcom/androlua/util/AsyncTaskX;->execute([Ljava/lang/Object;)Lcom/androlua/util/AsyncTaskX;

    return-object v7
.end method

.method public static post(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 8

    const-string v0, "[\\w\\-.:]+"

    invoke-virtual {p2, v0}, Ljava/lang/String;->matches(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_0

    invoke-static {p2}, Ljava/nio/charset/Charset;->isSupported(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_0

    new-instance v0, Lcom/androlua/Http$HttpTask;

    const-string v3, "POST"

    const/4 v4, 0x0

    const/4 v6, 0x0

    move-object v1, v0

    move-object v2, p0

    move-object v5, p2

    move-object v7, p3

    invoke-direct/range {v1 .. v7}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    goto :goto_0

    :cond_0
    new-instance v0, Lcom/androlua/Http$HttpTask;

    const-string v3, "POST"

    const/4 v6, 0x0

    const/4 v7, 0x0

    move-object v1, v0

    move-object v2, p0

    move-object v4, p2

    move-object v5, v6

    move-object v6, v7

    move-object v7, p3

    invoke-direct/range {v1 .. v7}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    :goto_0
    const/4 v1, 0x1

    new-array v1, v1, [Ljava/lang/Object;

    const/4 v2, 0x0

    aput-object p1, v1, v2

    invoke-virtual {v0, v1}, Lcom/androlua/util/AsyncTaskX;->execute([Ljava/lang/Object;)Lcom/androlua/util/AsyncTaskX;

    return-object v0
.end method

.method public static post(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 8

    new-instance v7, Lcom/androlua/Http$HttpTask;

    const-string v2, "POST"

    const/4 v5, 0x0

    move-object v0, v7

    move-object v1, p0

    move-object v3, p2

    move-object v4, p3

    move-object v6, p4

    invoke-direct/range {v0 .. v6}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    const/4 p0, 0x1

    new-array p0, p0, [Ljava/lang/Object;

    const/4 p2, 0x0

    aput-object p1, p0, p2

    invoke-virtual {v7, p0}, Lcom/androlua/util/AsyncTaskX;->execute([Ljava/lang/Object;)Lcom/androlua/util/AsyncTaskX;

    return-object v7
.end method

.method public static post(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 8
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Lcom/luajava/LuaObject;",
            ")",
            "Lcom/androlua/Http$HttpTask;"
        }
    .end annotation

    new-instance v7, Lcom/androlua/Http$HttpTask;

    const-string v2, "POST"

    move-object v0, v7

    move-object v1, p0

    move-object v3, p2

    move-object v4, p3

    move-object v5, p4

    move-object v6, p5

    invoke-direct/range {v0 .. v6}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    const/4 p0, 0x1

    new-array p0, p0, [Ljava/lang/Object;

    const/4 p2, 0x0

    aput-object p1, p0, p2

    invoke-virtual {v7, p0}, Lcom/androlua/util/AsyncTaskX;->execute([Ljava/lang/Object;)Lcom/androlua/util/AsyncTaskX;

    return-object v7
.end method

.method public static post(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 8
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Lcom/luajava/LuaObject;",
            ")",
            "Lcom/androlua/Http$HttpTask;"
        }
    .end annotation

    const-string v0, "[\\w\\-.:]+"

    invoke-virtual {p2, v0}, Ljava/lang/String;->matches(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_0

    invoke-static {p2}, Ljava/nio/charset/Charset;->isSupported(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_0

    new-instance v0, Lcom/androlua/Http$HttpTask;

    const-string v3, "POST"

    const/4 v4, 0x0

    move-object v1, v0

    move-object v2, p0

    move-object v5, p2

    move-object v6, p3

    move-object v7, p4

    invoke-direct/range {v1 .. v7}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    goto :goto_0

    :cond_0
    new-instance v0, Lcom/androlua/Http$HttpTask;

    const-string v3, "POST"

    const/4 v6, 0x0

    move-object v1, v0

    move-object v2, p0

    move-object v4, p2

    move-object v5, v6

    move-object v6, p3

    move-object v7, p4

    invoke-direct/range {v1 .. v7}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    :goto_0
    const/4 v1, 0x1

    new-array v1, v1, [Ljava/lang/Object;

    const/4 v2, 0x0

    aput-object p1, v1, v2

    invoke-virtual {v0, v1}, Lcom/androlua/util/AsyncTaskX;->execute([Ljava/lang/Object;)Lcom/androlua/util/AsyncTaskX;

    return-object v0
.end method

.method public static post(Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 8
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Lcom/luajava/LuaObject;",
            ")",
            "Lcom/androlua/Http$HttpTask;"
        }
    .end annotation

    new-instance v7, Lcom/androlua/Http$HttpTask;

    const-string v2, "POST"

    const/4 v3, 0x0

    const/4 v4, 0x0

    move-object v0, v7

    move-object v1, p0

    move-object v5, p2

    move-object v6, p3

    invoke-direct/range {v0 .. v6}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    const/4 p0, 0x1

    new-array p0, p0, [Ljava/lang/Object;

    const/4 p2, 0x0

    aput-object p1, p0, p2

    invoke-virtual {v7, p0}, Lcom/androlua/util/AsyncTaskX;->execute([Ljava/lang/Object;)Lcom/androlua/util/AsyncTaskX;

    return-object v7
.end method

.method public static post(Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 0
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/lang/String;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Lcom/luajava/LuaObject;",
            ")",
            "Lcom/androlua/Http$HttpTask;"
        }
    .end annotation

    invoke-static {p1}, Lcom/androlua/Http;->formatMap(Ljava/util/HashMap;)Ljava/lang/String;

    move-result-object p1

    invoke-static {p0, p1, p2}, Lcom/androlua/Http;->post(Ljava/lang/String;Ljava/lang/String;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;

    move-result-object p0

    return-object p0
.end method

.method public static post(Ljava/lang/String;Ljava/util/HashMap;Ljava/lang/String;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 0
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/lang/String;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Ljava/lang/String;",
            "Lcom/luajava/LuaObject;",
            ")",
            "Lcom/androlua/Http$HttpTask;"
        }
    .end annotation

    invoke-static {p1}, Lcom/androlua/Http;->formatMap(Ljava/util/HashMap;)Ljava/lang/String;

    move-result-object p1

    invoke-static {p0, p1, p2, p3}, Lcom/androlua/Http;->post(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;

    move-result-object p0

    return-object p0
.end method

.method public static post(Ljava/lang/String;Ljava/util/HashMap;Ljava/lang/String;Ljava/lang/String;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 0
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/lang/String;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            "Lcom/luajava/LuaObject;",
            ")",
            "Lcom/androlua/Http$HttpTask;"
        }
    .end annotation

    invoke-static {p1}, Lcom/androlua/Http;->formatMap(Ljava/util/HashMap;)Ljava/lang/String;

    move-result-object p1

    invoke-static {p0, p1, p2, p3, p4}, Lcom/androlua/Http;->post(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;

    move-result-object p0

    return-object p0
.end method

.method public static post(Ljava/lang/String;Ljava/util/HashMap;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 6
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/lang/String;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Lcom/luajava/LuaObject;",
            ")",
            "Lcom/androlua/Http$HttpTask;"
        }
    .end annotation

    invoke-static {p1}, Lcom/androlua/Http;->formatMap(Ljava/util/HashMap;)Ljava/lang/String;

    move-result-object v1

    move-object v0, p0

    move-object v2, p2

    move-object v3, p3

    move-object v4, p4

    move-object v5, p5

    invoke-static/range {v0 .. v5}, Lcom/androlua/Http;->post(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;

    move-result-object p0

    return-object p0
.end method

.method public static post(Ljava/lang/String;Ljava/util/HashMap;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 0
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/lang/String;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Ljava/lang/String;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Lcom/luajava/LuaObject;",
            ")",
            "Lcom/androlua/Http$HttpTask;"
        }
    .end annotation

    invoke-static {p1}, Lcom/androlua/Http;->formatMap(Ljava/util/HashMap;)Ljava/lang/String;

    move-result-object p1

    invoke-static {p0, p1, p2, p3, p4}, Lcom/androlua/Http;->post(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;

    move-result-object p0

    return-object p0
.end method

.method public static post(Ljava/lang/String;Ljava/util/HashMap;Ljava/util/HashMap;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 7
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/lang/String;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Lcom/luajava/LuaObject;",
            ")",
            "Lcom/androlua/Http$HttpTask;"
        }
    .end annotation

    const/4 v3, 0x0

    const/4 v4, 0x0

    const/4 v5, 0x0

    move-object v0, p0

    move-object v1, p1

    move-object v2, p2

    move-object v6, p3

    invoke-static/range {v0 .. v6}, Lcom/androlua/Http;->post(Ljava/lang/String;Ljava/util/HashMap;Ljava/util/HashMap;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;

    move-result-object p0

    return-object p0
.end method

.method public static post(Ljava/lang/String;Ljava/util/HashMap;Ljava/util/HashMap;Ljava/lang/String;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 6
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/lang/String;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Ljava/lang/String;",
            "Lcom/luajava/LuaObject;",
            ")",
            "Lcom/androlua/Http$HttpTask;"
        }
    .end annotation

    new-instance v4, Ljava/util/HashMap;

    invoke-direct {v4}, Ljava/util/HashMap;-><init>()V

    move-object v0, p0

    move-object v1, p1

    move-object v2, p2

    move-object v3, p3

    move-object v5, p4

    invoke-static/range {v0 .. v5}, Lcom/androlua/Http;->post(Ljava/lang/String;Ljava/util/HashMap;Ljava/util/HashMap;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;

    move-result-object p0

    return-object p0
.end method

.method public static post(Ljava/lang/String;Ljava/util/HashMap;Ljava/util/HashMap;Ljava/lang/String;Ljava/lang/String;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 7
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/lang/String;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            "Lcom/luajava/LuaObject;",
            ")",
            "Lcom/androlua/Http$HttpTask;"
        }
    .end annotation

    const/4 v5, 0x0

    move-object v0, p0

    move-object v1, p1

    move-object v2, p2

    move-object v3, p3

    move-object v4, p4

    move-object v6, p5

    invoke-static/range {v0 .. v6}, Lcom/androlua/Http;->post(Ljava/lang/String;Ljava/util/HashMap;Ljava/util/HashMap;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;

    move-result-object p0

    return-object p0
.end method

.method public static post(Ljava/lang/String;Ljava/util/HashMap;Ljava/util/HashMap;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 7
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/lang/String;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Lcom/luajava/LuaObject;",
            ")",
            "Lcom/androlua/Http$HttpTask;"
        }
    .end annotation

    if-nez p5, :cond_0

    new-instance p5, Ljava/util/HashMap;

    invoke-direct {p5}, Ljava/util/HashMap;-><init>()V

    :cond_0
    move-object v5, p5

    const-string p5, "Content-Type"

    const-string v0, "multipart/form-data;boundary=----q1w2e3r4t5y6u7i8o9p0a1s2d3f4g5h6j7k8l9z0x1c2v3b4n5m6"

    invoke-virtual {v5, p5, v0}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    new-instance p5, Lcom/androlua/Http$HttpTask;

    const-string v2, "POST"

    move-object v0, p5

    move-object v1, p0

    move-object v3, p3

    move-object v4, p4

    move-object v6, p6

    invoke-direct/range {v0 .. v6}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    const/4 p0, 0x1

    new-array p0, p0, [Ljava/lang/Object;

    const/4 p3, 0x0

    invoke-static {p1, p2, p4}, Lcom/androlua/Http;->formatMultiDate(Ljava/util/HashMap;Ljava/util/HashMap;Ljava/lang/String;)[B

    move-result-object p1

    aput-object p1, p0, p3

    invoke-virtual {p5, p0}, Lcom/androlua/util/AsyncTaskX;->execute([Ljava/lang/Object;)Lcom/androlua/util/AsyncTaskX;

    return-object p5
.end method

.method public static post(Ljava/lang/String;Ljava/util/HashMap;Ljava/util/HashMap;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 8
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/lang/String;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Ljava/lang/String;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Lcom/luajava/LuaObject;",
            ")",
            "Lcom/androlua/Http$HttpTask;"
        }
    .end annotation

    const-string v0, "[\\w\\-.:]+"

    invoke-virtual {p3, v0}, Ljava/lang/String;->matches(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_0

    invoke-static {p3}, Ljava/nio/charset/Charset;->isSupported(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_0

    const/4 v5, 0x0

    move-object v1, p0

    move-object v2, p1

    move-object v3, p2

    move-object v4, p3

    move-object v6, p4

    move-object v7, p5

    invoke-static/range {v1 .. v7}, Lcom/androlua/Http;->post(Ljava/lang/String;Ljava/util/HashMap;Ljava/util/HashMap;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;

    move-result-object p0

    goto :goto_0

    :cond_0
    const/4 v3, 0x0

    move-object v0, p0

    move-object v1, p1

    move-object v2, p2

    move-object v4, p3

    move-object v5, p4

    move-object v6, p5

    invoke-static/range {v0 .. v6}, Lcom/androlua/Http;->post(Ljava/lang/String;Ljava/util/HashMap;Ljava/util/HashMap;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;

    move-result-object p0

    :goto_0
    return-object p0
.end method

.method public static post(Ljava/lang/String;Ljava/util/HashMap;Ljava/util/HashMap;Ljava/util/HashMap;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 6
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/lang/String;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Lcom/luajava/LuaObject;",
            ")",
            "Lcom/androlua/Http$HttpTask;"
        }
    .end annotation

    const/4 v3, 0x0

    move-object v0, p0

    move-object v1, p1

    move-object v2, p2

    move-object v4, p3

    move-object v5, p4

    invoke-static/range {v0 .. v5}, Lcom/androlua/Http;->post(Ljava/lang/String;Ljava/util/HashMap;Ljava/util/HashMap;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;

    move-result-object p0

    return-object p0
.end method

.method public static put(Ljava/lang/String;Ljava/lang/String;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 8

    new-instance v7, Lcom/androlua/Http$HttpTask;

    const-string v2, "PUT"

    const/4 v3, 0x0

    const/4 v4, 0x0

    const/4 v5, 0x0

    move-object v0, v7

    move-object v1, p0

    move-object v6, p2

    invoke-direct/range {v0 .. v6}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    const/4 p0, 0x1

    new-array p0, p0, [Ljava/lang/Object;

    const/4 p2, 0x0

    aput-object p1, p0, p2

    invoke-virtual {v7, p0}, Lcom/androlua/util/AsyncTaskX;->execute([Ljava/lang/Object;)Lcom/androlua/util/AsyncTaskX;

    return-object v7
.end method

.method public static put(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 8

    const-string v0, "[\\w\\-\\.:]+"

    invoke-virtual {p2, v0}, Ljava/lang/String;->matches(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_0

    invoke-static {p2}, Ljava/nio/charset/Charset;->isSupported(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_0

    new-instance v0, Lcom/androlua/Http$HttpTask;

    const-string v3, "PUT"

    const/4 v4, 0x0

    const/4 v6, 0x0

    move-object v1, v0

    move-object v2, p0

    move-object v5, p2

    move-object v7, p3

    invoke-direct/range {v1 .. v7}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    goto :goto_0

    :cond_0
    new-instance v0, Lcom/androlua/Http$HttpTask;

    const-string v3, "PUT"

    const/4 v6, 0x0

    const/4 v7, 0x0

    move-object v1, v0

    move-object v2, p0

    move-object v4, p2

    move-object v5, v6

    move-object v6, v7

    move-object v7, p3

    invoke-direct/range {v1 .. v7}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    :goto_0
    const/4 v1, 0x1

    new-array v1, v1, [Ljava/lang/Object;

    const/4 v2, 0x0

    aput-object p1, v1, v2

    invoke-virtual {v0, v1}, Lcom/androlua/util/AsyncTaskX;->execute([Ljava/lang/Object;)Lcom/androlua/util/AsyncTaskX;

    return-object v0
.end method

.method public static put(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 8

    new-instance v7, Lcom/androlua/Http$HttpTask;

    const-string v2, "PUT"

    const/4 v5, 0x0

    move-object v0, v7

    move-object v1, p0

    move-object v3, p2

    move-object v4, p3

    move-object v6, p4

    invoke-direct/range {v0 .. v6}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    const/4 p0, 0x1

    new-array p0, p0, [Ljava/lang/Object;

    const/4 p2, 0x0

    aput-object p1, p0, p2

    invoke-virtual {v7, p0}, Lcom/androlua/util/AsyncTaskX;->execute([Ljava/lang/Object;)Lcom/androlua/util/AsyncTaskX;

    return-object v7
.end method

.method public static put(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 8
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Lcom/luajava/LuaObject;",
            ")",
            "Lcom/androlua/Http$HttpTask;"
        }
    .end annotation

    new-instance v7, Lcom/androlua/Http$HttpTask;

    const-string v2, "PUT"

    move-object v0, v7

    move-object v1, p0

    move-object v3, p2

    move-object v4, p3

    move-object v5, p4

    move-object v6, p5

    invoke-direct/range {v0 .. v6}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    const/4 p0, 0x1

    new-array p0, p0, [Ljava/lang/Object;

    const/4 p2, 0x0

    aput-object p1, p0, p2

    invoke-virtual {v7, p0}, Lcom/androlua/util/AsyncTaskX;->execute([Ljava/lang/Object;)Lcom/androlua/util/AsyncTaskX;

    return-object v7
.end method

.method public static put(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 8
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Lcom/luajava/LuaObject;",
            ")",
            "Lcom/androlua/Http$HttpTask;"
        }
    .end annotation

    const-string v0, "[\\w\\-\\.:]+"

    invoke-virtual {p2, v0}, Ljava/lang/String;->matches(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_0

    invoke-static {p2}, Ljava/nio/charset/Charset;->isSupported(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_0

    new-instance v0, Lcom/androlua/Http$HttpTask;

    const-string v3, "PUT"

    const/4 v4, 0x0

    move-object v1, v0

    move-object v2, p0

    move-object v5, p2

    move-object v6, p3

    move-object v7, p4

    invoke-direct/range {v1 .. v7}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    goto :goto_0

    :cond_0
    new-instance v0, Lcom/androlua/Http$HttpTask;

    const-string v3, "PUT"

    const/4 v6, 0x0

    move-object v1, v0

    move-object v2, p0

    move-object v4, p2

    move-object v5, v6

    move-object v6, p3

    move-object v7, p4

    invoke-direct/range {v1 .. v7}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    :goto_0
    const/4 v1, 0x1

    new-array v1, v1, [Ljava/lang/Object;

    const/4 v2, 0x0

    aput-object p1, v1, v2

    invoke-virtual {v0, v1}, Lcom/androlua/util/AsyncTaskX;->execute([Ljava/lang/Object;)Lcom/androlua/util/AsyncTaskX;

    return-object v0
.end method

.method public static put(Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)Lcom/androlua/Http$HttpTask;
    .locals 8
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;",
            "Lcom/luajava/LuaObject;",
            ")",
            "Lcom/androlua/Http$HttpTask;"
        }
    .end annotation

    new-instance v7, Lcom/androlua/Http$HttpTask;

    const-string v2, "PUT"

    const/4 v3, 0x0

    const/4 v4, 0x0

    move-object v0, v7

    move-object v1, p0

    move-object v5, p2

    move-object v6, p3

    invoke-direct/range {v0 .. v6}, Lcom/androlua/Http$HttpTask;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/HashMap;Lcom/luajava/LuaObject;)V

    const/4 p0, 0x1

    new-array p0, p0, [Ljava/lang/Object;

    const/4 p2, 0x0

    aput-object p1, p0, p2

    invoke-virtual {v7, p0}, Lcom/androlua/util/AsyncTaskX;->execute([Ljava/lang/Object;)Lcom/androlua/util/AsyncTaskX;

    return-object v7
.end method

.method public static setCookie(Ljava/lang/String;)V
    .locals 2

    sget-object v0, Lcom/androlua/Http;->sHeader:Ljava/util/HashMap;

    if-nez v0, :cond_0

    new-instance v0, Ljava/util/HashMap;

    invoke-direct {v0}, Ljava/util/HashMap;-><init>()V

    sput-object v0, Lcom/androlua/Http;->sHeader:Ljava/util/HashMap;

    :cond_0
    sget-object v0, Lcom/androlua/Http;->sHeader:Ljava/util/HashMap;

    const-string v1, "Cookie"

    invoke-virtual {v0, v1, p0}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    return-void
.end method

.method public static setHeader(Ljava/util/HashMap;)V
    .locals 0
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;)V"
        }
    .end annotation

    sput-object p0, Lcom/androlua/Http;->sHeader:Ljava/util/HashMap;

    return-void
.end method

.method public static setReferer(Ljava/lang/String;)V
    .locals 2

    sget-object v0, Lcom/androlua/Http;->sHeader:Ljava/util/HashMap;

    if-nez v0, :cond_0

    new-instance v0, Ljava/util/HashMap;

    invoke-direct {v0}, Ljava/util/HashMap;-><init>()V

    sput-object v0, Lcom/androlua/Http;->sHeader:Ljava/util/HashMap;

    :cond_0
    sget-object v0, Lcom/androlua/Http;->sHeader:Ljava/util/HashMap;

    const-string v1, "Referer"

    invoke-virtual {v0, v1, p0}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    return-void
.end method

.method public static setUserAgent(Ljava/lang/String;)V
    .locals 2

    sget-object v0, Lcom/androlua/Http;->sHeader:Ljava/util/HashMap;

    if-nez v0, :cond_0

    new-instance v0, Ljava/util/HashMap;

    invoke-direct {v0}, Ljava/util/HashMap;-><init>()V

    sput-object v0, Lcom/androlua/Http;->sHeader:Ljava/util/HashMap;

    :cond_0
    sget-object v0, Lcom/androlua/Http;->sHeader:Ljava/util/HashMap;

    const-string v1, "User-Agent"

    invoke-virtual {v0, v1, p0}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    return-void
.end method
