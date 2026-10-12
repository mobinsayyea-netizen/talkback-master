.class public Lcom/androlua/LuaTimerTask;
.super Lcom/androlua/util/TimerTaskX;
.source "SourceFile"


# instance fields
.field private L:Lcom/luajava/LuaState;

.field private mArg:[Ljava/lang/Object;

.field private mBuffer:[B

.field private mEnabled:Z

.field private mLuaContext:Lcom/androlua/LuaContext;

.field private mSrc:Ljava/lang/String;


# direct methods
.method public constructor <init>(Lcom/androlua/LuaContext;Lcom/luajava/LuaObject;)V
    .locals 1

    const/4 v0, 0x0

    invoke-direct {p0, p1, p2, v0}, Lcom/androlua/LuaTimerTask;-><init>(Lcom/androlua/LuaContext;Lcom/luajava/LuaObject;[Ljava/lang/Object;)V

    return-void
.end method

.method public constructor <init>(Lcom/androlua/LuaContext;Lcom/luajava/LuaObject;[Ljava/lang/Object;)V
    .locals 1

    invoke-direct {p0}, Lcom/androlua/util/TimerTaskX;-><init>()V

    const/4 v0, 0x0

    new-array v0, v0, [Ljava/lang/Object;

    iput-object v0, p0, Lcom/androlua/LuaTimerTask;->mArg:[Ljava/lang/Object;

    const/4 v0, 0x1

    iput-boolean v0, p0, Lcom/androlua/LuaTimerTask;->mEnabled:Z

    iput-object p1, p0, Lcom/androlua/LuaTimerTask;->mLuaContext:Lcom/androlua/LuaContext;

    if-eqz p3, :cond_0

    iput-object p3, p0, Lcom/androlua/LuaTimerTask;->mArg:[Ljava/lang/Object;

    :cond_0
    invoke-virtual {p2}, Lcom/luajava/LuaObject;->dump()[B

    move-result-object p1

    iput-object p1, p0, Lcom/androlua/LuaTimerTask;->mBuffer:[B

    return-void
.end method

.method public constructor <init>(Lcom/androlua/LuaContext;Ljava/lang/String;)V
    .locals 1

    const/4 v0, 0x0

    invoke-direct {p0, p1, p2, v0}, Lcom/androlua/LuaTimerTask;-><init>(Lcom/androlua/LuaContext;Ljava/lang/String;[Ljava/lang/Object;)V

    return-void
.end method

.method public constructor <init>(Lcom/androlua/LuaContext;Ljava/lang/String;[Ljava/lang/Object;)V
    .locals 1

    invoke-direct {p0}, Lcom/androlua/util/TimerTaskX;-><init>()V

    const/4 v0, 0x0

    new-array v0, v0, [Ljava/lang/Object;

    iput-object v0, p0, Lcom/androlua/LuaTimerTask;->mArg:[Ljava/lang/Object;

    const/4 v0, 0x1

    iput-boolean v0, p0, Lcom/androlua/LuaTimerTask;->mEnabled:Z

    iput-object p1, p0, Lcom/androlua/LuaTimerTask;->mLuaContext:Lcom/androlua/LuaContext;

    iput-object p2, p0, Lcom/androlua/LuaTimerTask;->mSrc:Ljava/lang/String;

    if-eqz p3, :cond_0

    iput-object p3, p0, Lcom/androlua/LuaTimerTask;->mArg:[Ljava/lang/Object;

    :cond_0
    return-void
.end method

.method static bridge synthetic a(Lcom/androlua/LuaTimerTask;)Lcom/androlua/LuaContext;
    .locals 0

    iget-object p0, p0, Lcom/androlua/LuaTimerTask;->mLuaContext:Lcom/androlua/LuaContext;

    return-object p0
.end method

.method private varargs doFile(Ljava/lang/String;[Ljava/lang/Object;)V
    .locals 5

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const/4 v1, 0x0

    invoke-virtual {v0, v1}, Lcom/luajava/LuaState;->p0(I)V

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    invoke-virtual {v0, p1}, Lcom/luajava/LuaState;->b(Ljava/lang/String;)I

    move-result p1

    const/4 v0, -0x1

    if-nez p1, :cond_1

    iget-object p1, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const-string v2, "debug"

    invoke-virtual {p1, v2}, Lcom/luajava/LuaState;->p(Ljava/lang/String;)I

    iget-object p1, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const-string v2, "traceback"

    invoke-virtual {p1, v0, v2}, Lcom/luajava/LuaState;->m(ILjava/lang/String;)I

    iget-object p1, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const/4 v2, -0x2

    invoke-virtual {p1, v2}, Lcom/luajava/LuaState;->j0(I)V

    iget-object p1, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    invoke-virtual {p1, v2}, Lcom/luajava/LuaState;->A(I)V

    array-length p1, p2

    move v2, v1

    :goto_0
    if-ge v2, p1, :cond_0

    iget-object v3, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    aget-object v4, p2, v2

    invoke-virtual {v3, v4}, Lcom/luajava/LuaState;->a0(Ljava/lang/Object;)V

    add-int/lit8 v2, v2, 0x1

    goto :goto_0

    :cond_0
    iget-object p2, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    rsub-int/lit8 v2, p1, -0x2

    invoke-virtual {p2, p1, v1, v2}, Lcom/luajava/LuaState;->P(III)I

    move-result p1

    if-nez p1, :cond_1

    return-void

    :cond_1
    new-instance p2, Lcom/luajava/LuaException;

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    invoke-direct {p0, p1}, Lcom/androlua/LuaTimerTask;->errorReason(I)Ljava/lang/String;

    move-result-object p1

    invoke-virtual {v1, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string p1, ": "

    invoke-virtual {v1, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object p1, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    invoke-virtual {p1, v0}, Lcom/luajava/LuaState;->v0(I)Ljava/lang/String;

    move-result-object p1

    invoke-virtual {v1, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-direct {p2, p1}, Lcom/luajava/LuaException;-><init>(Ljava/lang/String;)V

    throw p2
.end method

.method private varargs doString(Ljava/lang/String;[Ljava/lang/Object;)V
    .locals 5

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const/4 v1, 0x0

    invoke-virtual {v0, v1}, Lcom/luajava/LuaState;->p0(I)V

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    invoke-virtual {v0, p1}, Lcom/luajava/LuaState;->c(Ljava/lang/String;)I

    move-result p1

    const/4 v0, -0x1

    if-nez p1, :cond_1

    iget-object p1, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const-string v2, "debug"

    invoke-virtual {p1, v2}, Lcom/luajava/LuaState;->p(Ljava/lang/String;)I

    iget-object p1, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const-string v2, "traceback"

    invoke-virtual {p1, v0, v2}, Lcom/luajava/LuaState;->m(ILjava/lang/String;)I

    iget-object p1, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const/4 v2, -0x2

    invoke-virtual {p1, v2}, Lcom/luajava/LuaState;->j0(I)V

    iget-object p1, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    invoke-virtual {p1, v2}, Lcom/luajava/LuaState;->A(I)V

    array-length p1, p2

    move v2, v1

    :goto_0
    if-ge v2, p1, :cond_0

    iget-object v3, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    aget-object v4, p2, v2

    invoke-virtual {v3, v4}, Lcom/luajava/LuaState;->a0(Ljava/lang/Object;)V

    add-int/lit8 v2, v2, 0x1

    goto :goto_0

    :cond_0
    iget-object p2, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    rsub-int/lit8 v2, p1, -0x2

    invoke-virtual {p2, p1, v1, v2}, Lcom/luajava/LuaState;->P(III)I

    move-result p1

    if-nez p1, :cond_1

    return-void

    :cond_1
    new-instance p2, Lcom/luajava/LuaException;

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    invoke-direct {p0, p1}, Lcom/androlua/LuaTimerTask;->errorReason(I)Ljava/lang/String;

    move-result-object p1

    invoke-virtual {v1, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string p1, ": "

    invoke-virtual {v1, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object p1, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    invoke-virtual {p1, v0}, Lcom/luajava/LuaState;->v0(I)Ljava/lang/String;

    move-result-object p1

    invoke-virtual {v1, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-direct {p2, p1}, Lcom/luajava/LuaException;-><init>(Ljava/lang/String;)V

    throw p2
.end method

.method private errorReason(I)Ljava/lang/String;
    .locals 2

    packed-switch p1, :pswitch_data_0

    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    const-string v1, "Unknown error "

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    return-object p1

    :pswitch_0
    const-string p1, "error error"

    return-object p1

    :pswitch_1
    const-string p1, "GC error"

    return-object p1

    :pswitch_2
    const-string p1, "Out of memory"

    return-object p1

    :pswitch_3
    const-string p1, "Syntax error"

    return-object p1

    :pswitch_4
    const-string p1, "Runtime error"

    return-object p1

    :pswitch_5
    const-string p1, "Yield error"

    return-object p1

    nop

    :pswitch_data_0
    .packed-switch 0x1
        :pswitch_5
        :pswitch_4
        :pswitch_3
        :pswitch_2
        :pswitch_1
        :pswitch_0
    .end packed-switch
.end method

.method private initLua()V
    .locals 3

    invoke-static {}, Lcom/luajava/LuaStateFactory;->c()Lcom/luajava/LuaState;

    move-result-object v0

    iput-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    invoke-virtual {v0}, Lcom/luajava/LuaState;->O()V

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    iget-object v1, p0, Lcom/androlua/LuaTimerTask;->mLuaContext:Lcom/androlua/LuaContext;

    invoke-virtual {v0, v1}, Lcom/luajava/LuaState;->X(Ljava/lang/Object;)V

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->mLuaContext:Lcom/androlua/LuaContext;

    instance-of v1, v0, Lcom/androlua/LuaActivity;

    if-eqz v1, :cond_0

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const-string v1, "activity"

    :goto_0
    invoke-virtual {v0, v1}, Lcom/luajava/LuaState;->m0(Ljava/lang/String;)V

    goto :goto_1

    :cond_0
    instance-of v0, v0, Lcom/androlua/LuaService;

    if-eqz v0, :cond_1

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const-string v1, "service"

    goto :goto_0

    :cond_1
    :goto_1
    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    invoke-virtual {v0, p0}, Lcom/luajava/LuaState;->X(Ljava/lang/Object;)V

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const-string v1, "this"

    invoke-virtual {v0, v1}, Lcom/luajava/LuaState;->m0(Ljava/lang/String;)V

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    iget-object v1, p0, Lcom/androlua/LuaTimerTask;->mLuaContext:Lcom/androlua/LuaContext;

    invoke-virtual {v0, v1}, Lcom/luajava/LuaState;->S(Lcom/androlua/LuaContext;)V

    new-instance v0, Lcom/androlua/LuaPrint;

    iget-object v1, p0, Lcom/androlua/LuaTimerTask;->mLuaContext:Lcom/androlua/LuaContext;

    iget-object v2, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    invoke-direct {v0, v1, v2}, Lcom/androlua/LuaPrint;-><init>(Lcom/androlua/LuaContext;Lcom/luajava/LuaState;)V

    const-string v1, "print"

    invoke-virtual {v0, v1}, Lcom/luajava/JavaFunction;->register(Ljava/lang/String;)V

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const-string v1, "package"

    invoke-virtual {v0, v1}, Lcom/luajava/LuaState;->p(Ljava/lang/String;)I

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    iget-object v1, p0, Lcom/androlua/LuaTimerTask;->mLuaContext:Lcom/androlua/LuaContext;

    invoke-interface {v1}, Lcom/androlua/LuaContext;->getLuaLpath()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v0, v1}, Lcom/luajava/LuaState;->c0(Ljava/lang/String;)V

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const-string v1, "path"

    const/4 v2, -0x2

    invoke-virtual {v0, v2, v1}, Lcom/luajava/LuaState;->l0(ILjava/lang/String;)V

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    iget-object v1, p0, Lcom/androlua/LuaTimerTask;->mLuaContext:Lcom/androlua/LuaContext;

    invoke-interface {v1}, Lcom/androlua/LuaContext;->getLuaCpath()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v0, v1}, Lcom/luajava/LuaState;->c0(Ljava/lang/String;)V

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const-string v1, "cpath"

    invoke-virtual {v0, v2, v1}, Lcom/luajava/LuaState;->l0(ILjava/lang/String;)V

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const/4 v1, 0x1

    invoke-virtual {v0, v1}, Lcom/luajava/LuaState;->Q(I)V

    new-instance v0, Lcom/androlua/LuaTimerTask$1;

    iget-object v1, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    invoke-direct {v0, p0, v1}, Lcom/androlua/LuaTimerTask$1;-><init>(Lcom/androlua/LuaTimerTask;Lcom/luajava/LuaState;)V

    const-string v1, "set"

    invoke-virtual {v0, v1}, Lcom/luajava/JavaFunction;->register(Ljava/lang/String;)V

    new-instance v0, Lcom/androlua/LuaTimerTask$2;

    iget-object v1, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    invoke-direct {v0, p0, v1}, Lcom/androlua/LuaTimerTask$2;-><init>(Lcom/androlua/LuaTimerTask;Lcom/luajava/LuaState;)V

    const-string v1, "call"

    invoke-virtual {v0, v1}, Lcom/luajava/JavaFunction;->register(Ljava/lang/String;)V

    return-void
.end method

.method private varargs newLuaThread(Ljava/lang/String;[Ljava/lang/Object;)V
    .locals 3

    :try_start_0
    const-string v0, "^\\w+$"

    invoke-static {v0, p1}, Ljava/util/regex/Pattern;->matches(Ljava/lang/String;Ljava/lang/CharSequence;)Z

    move-result v0

    if-eqz v0, :cond_0

    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string p1, ".lua"

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-virtual {p0, p1, p2}, Lcom/androlua/LuaTimerTask;->doAsset(Ljava/lang/String;[Ljava/lang/Object;)V

    goto :goto_0

    :cond_0
    const-string v0, "^[\\w._/]+$"

    invoke-static {v0, p1}, Ljava/util/regex/Pattern;->matches(Ljava/lang/String;Ljava/lang/CharSequence;)Z

    move-result v0

    if-eqz v0, :cond_1

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const-string v1, "luajava"

    invoke-virtual {v0, v1}, Lcom/luajava/LuaState;->p(Ljava/lang/String;)I

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    iget-object v1, p0, Lcom/androlua/LuaTimerTask;->mLuaContext:Lcom/androlua/LuaContext;

    invoke-interface {v1}, Lcom/androlua/LuaContext;->getLuaDir()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v0, v1}, Lcom/luajava/LuaState;->c0(Ljava/lang/String;)V

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const-string v1, "luadir"

    const/4 v2, -0x2

    invoke-virtual {v0, v2, v1}, Lcom/luajava/LuaState;->l0(ILjava/lang/String;)V

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    invoke-virtual {v0, p1}, Lcom/luajava/LuaState;->c0(Ljava/lang/String;)V

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const-string v1, "luapath"

    invoke-virtual {v0, v2, v1}, Lcom/luajava/LuaState;->l0(ILjava/lang/String;)V

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const/4 v1, 0x1

    invoke-virtual {v0, v1}, Lcom/luajava/LuaState;->Q(I)V

    invoke-direct {p0, p1, p2}, Lcom/androlua/LuaTimerTask;->doFile(Ljava/lang/String;[Ljava/lang/Object;)V

    goto :goto_0

    :cond_1
    invoke-direct {p0, p1, p2}, Lcom/androlua/LuaTimerTask;->doString(Ljava/lang/String;[Ljava/lang/Object;)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_0

    :catch_0
    move-exception p1

    iget-object p2, p0, Lcom/androlua/LuaTimerTask;->mLuaContext:Lcom/androlua/LuaContext;

    invoke-virtual {p0}, Ljava/lang/Object;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-interface {p2, v0, p1}, Lcom/androlua/LuaContext;->sendError(Ljava/lang/String;Ljava/lang/Exception;)V

    :goto_0
    return-void
.end method

.method private varargs newLuaThread([B[Ljava/lang/Object;)V
    .locals 5

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const/4 v1, 0x0

    invoke-virtual {v0, v1}, Lcom/luajava/LuaState;->p0(I)V

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const-string v2, "TimerTask"

    invoke-virtual {v0, p1, v2}, Lcom/luajava/LuaState;->a([BLjava/lang/String;)I

    move-result p1

    const/4 v0, -0x1

    if-nez p1, :cond_1

    iget-object p1, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const-string v2, "debug"

    invoke-virtual {p1, v2}, Lcom/luajava/LuaState;->p(Ljava/lang/String;)I

    iget-object p1, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const-string v2, "traceback"

    invoke-virtual {p1, v0, v2}, Lcom/luajava/LuaState;->m(ILjava/lang/String;)I

    iget-object p1, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const/4 v2, -0x2

    invoke-virtual {p1, v2}, Lcom/luajava/LuaState;->j0(I)V

    iget-object p1, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    invoke-virtual {p1, v2}, Lcom/luajava/LuaState;->A(I)V

    array-length p1, p2

    move v2, v1

    :goto_0
    if-ge v2, p1, :cond_0

    iget-object v3, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    aget-object v4, p2, v2

    invoke-virtual {v3, v4}, Lcom/luajava/LuaState;->a0(Ljava/lang/Object;)V

    add-int/lit8 v2, v2, 0x1

    goto :goto_0

    :cond_0
    iget-object p2, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    rsub-int/lit8 v2, p1, -0x2

    invoke-virtual {p2, p1, v1, v2}, Lcom/luajava/LuaState;->P(III)I

    move-result p1

    if-nez p1, :cond_1

    return-void

    :cond_1
    new-instance p2, Lcom/luajava/LuaException;

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    invoke-direct {p0, p1}, Lcom/androlua/LuaTimerTask;->errorReason(I)Ljava/lang/String;

    move-result-object p1

    invoke-virtual {v1, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string p1, ": "

    invoke-virtual {v1, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object p1, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    invoke-virtual {p1, v0}, Lcom/luajava/LuaState;->v0(I)Ljava/lang/String;

    move-result-object p1

    invoke-virtual {v1, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-direct {p2, p1}, Lcom/luajava/LuaException;-><init>(Ljava/lang/String;)V

    throw p2
.end method

.method private varargs runFunc(Ljava/lang/String;[Ljava/lang/Object;)V
    .locals 5

    :try_start_0
    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const/4 v1, 0x0

    invoke-virtual {v0, v1}, Lcom/luajava/LuaState;->p0(I)V

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    invoke-virtual {v0, p1}, Lcom/luajava/LuaState;->p(Ljava/lang/String;)I

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const/4 v2, -0x1

    invoke-virtual {v0, v2}, Lcom/luajava/LuaState;->C(I)Z

    move-result v0

    if-eqz v0, :cond_2

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const-string v3, "debug"

    invoke-virtual {v0, v3}, Lcom/luajava/LuaState;->p(Ljava/lang/String;)I

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const-string v3, "traceback"

    invoke-virtual {v0, v2, v3}, Lcom/luajava/LuaState;->m(ILjava/lang/String;)I

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const/4 v3, -0x2

    invoke-virtual {v0, v3}, Lcom/luajava/LuaState;->j0(I)V

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    invoke-virtual {v0, v3}, Lcom/luajava/LuaState;->A(I)V

    array-length v0, p2

    :goto_0
    if-ge v1, v0, :cond_0

    iget-object v3, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    aget-object v4, p2, v1

    invoke-virtual {v3, v4}, Lcom/luajava/LuaState;->a0(Ljava/lang/Object;)V

    add-int/lit8 v1, v1, 0x1

    goto :goto_0

    :cond_0
    iget-object p2, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    rsub-int/lit8 v1, v0, -0x2

    const/4 v3, 0x1

    invoke-virtual {p2, v0, v3, v1}, Lcom/luajava/LuaState;->P(III)I

    move-result p2

    if-nez p2, :cond_1

    return-void

    :cond_1
    new-instance v0, Lcom/luajava/LuaException;

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    invoke-direct {p0, p2}, Lcom/androlua/LuaTimerTask;->errorReason(I)Ljava/lang/String;

    move-result-object p2

    invoke-virtual {v1, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string p2, ": "

    invoke-virtual {v1, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object p2, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    invoke-virtual {p2, v2}, Lcom/luajava/LuaState;->v0(I)Ljava/lang/String;

    move-result-object p2

    invoke-virtual {v1, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p2

    invoke-direct {v0, p2}, Lcom/luajava/LuaException;-><init>(Ljava/lang/String;)V

    throw v0
    :try_end_0
    .catch Lcom/luajava/LuaException; {:try_start_0 .. :try_end_0} :catch_0

    :catch_0
    move-exception p2

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->mLuaContext:Lcom/androlua/LuaContext;

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {p0}, Ljava/lang/Object;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v2, " "

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-interface {v0, p1, p2}, Lcom/androlua/LuaContext;->sendError(Ljava/lang/String;Ljava/lang/Exception;)V

    :cond_2
    return-void
.end method

.method private setField(Ljava/lang/String;Ljava/lang/Object;)V
    .locals 1

    :try_start_0
    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    invoke-virtual {v0, p2}, Lcom/luajava/LuaState;->a0(Ljava/lang/Object;)V

    iget-object p2, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    invoke-virtual {p2, p1}, Lcom/luajava/LuaState;->m0(Ljava/lang/String;)V
    :try_end_0
    .catch Lcom/luajava/LuaException; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_0

    :catch_0
    move-exception p1

    iget-object p2, p0, Lcom/androlua/LuaTimerTask;->mLuaContext:Lcom/androlua/LuaContext;

    invoke-virtual {p0}, Ljava/lang/Object;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-interface {p2, v0, p1}, Lcom/androlua/LuaContext;->sendError(Ljava/lang/String;Ljava/lang/Exception;)V

    :goto_0
    return-void
.end method


# virtual methods
.method public cancel()Z
    .locals 1

    invoke-super {p0}, Lcom/androlua/util/TimerTaskX;->cancel()Z

    move-result v0

    return v0
.end method

.method public varargs doAsset(Ljava/lang/String;[Ljava/lang/Object;)V
    .locals 5

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->mLuaContext:Lcom/androlua/LuaContext;

    invoke-interface {v0}, Lcom/androlua/LuaContext;->getContext()Landroid/content/Context;

    move-result-object v0

    invoke-static {v0, p1}, Lcom/androlua/LuaUtil;->readAsset(Landroid/content/Context;Ljava/lang/String;)[B

    move-result-object v0

    iget-object v1, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const/4 v2, 0x0

    invoke-virtual {v1, v2}, Lcom/luajava/LuaState;->p0(I)V

    iget-object v1, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    invoke-virtual {v1, v0, p1}, Lcom/luajava/LuaState;->a([BLjava/lang/String;)I

    move-result p1

    const/4 v0, -0x1

    if-nez p1, :cond_1

    iget-object p1, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const-string v1, "debug"

    invoke-virtual {p1, v1}, Lcom/luajava/LuaState;->p(Ljava/lang/String;)I

    iget-object p1, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const-string v1, "traceback"

    invoke-virtual {p1, v0, v1}, Lcom/luajava/LuaState;->m(ILjava/lang/String;)I

    iget-object p1, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const/4 v1, -0x2

    invoke-virtual {p1, v1}, Lcom/luajava/LuaState;->j0(I)V

    iget-object p1, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    invoke-virtual {p1, v1}, Lcom/luajava/LuaState;->A(I)V

    array-length p1, p2

    move v1, v2

    :goto_0
    if-ge v1, p1, :cond_0

    iget-object v3, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    aget-object v4, p2, v1

    invoke-virtual {v3, v4}, Lcom/luajava/LuaState;->a0(Ljava/lang/Object;)V

    add-int/lit8 v1, v1, 0x1

    goto :goto_0

    :cond_0
    iget-object p2, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    rsub-int/lit8 v1, p1, -0x2

    invoke-virtual {p2, p1, v2, v1}, Lcom/luajava/LuaState;->P(III)I

    move-result p1

    if-nez p1, :cond_1

    return-void

    :cond_1
    new-instance p2, Lcom/luajava/LuaException;

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    invoke-direct {p0, p1}, Lcom/androlua/LuaTimerTask;->errorReason(I)Ljava/lang/String;

    move-result-object p1

    invoke-virtual {v1, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string p1, ": "

    invoke-virtual {v1, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object p1, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    invoke-virtual {p1, v0}, Lcom/luajava/LuaState;->v0(I)Ljava/lang/String;

    move-result-object p1

    invoke-virtual {v1, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-direct {p2, p1}, Lcom/luajava/LuaException;-><init>(Ljava/lang/String;)V

    throw p2
.end method

.method public get(Ljava/lang/String;)Ljava/lang/Object;
    .locals 1

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    invoke-virtual {v0, p1}, Lcom/luajava/LuaState;->p(Ljava/lang/String;)I

    iget-object p1, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const/4 v0, -0x1

    invoke-virtual {p1, v0}, Lcom/luajava/LuaState;->t0(I)Ljava/lang/Object;

    move-result-object p1

    return-object p1
.end method

.method public isEnabled()Z
    .locals 1

    iget-boolean v0, p0, Lcom/androlua/LuaTimerTask;->mEnabled:Z

    return v0
.end method

.method public run()V
    .locals 3

    const-string v0, "run"

    iget-boolean v1, p0, Lcom/androlua/LuaTimerTask;->mEnabled:Z

    if-nez v1, :cond_0

    return-void

    :cond_0
    :try_start_0
    iget-object v1, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    if-nez v1, :cond_2

    invoke-direct {p0}, Lcom/androlua/LuaTimerTask;->initLua()V

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->mBuffer:[B

    if-eqz v0, :cond_1

    iget-object v1, p0, Lcom/androlua/LuaTimerTask;->mArg:[Ljava/lang/Object;

    invoke-direct {p0, v0, v1}, Lcom/androlua/LuaTimerTask;->newLuaThread([B[Ljava/lang/Object;)V

    goto :goto_0

    :cond_1
    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->mSrc:Ljava/lang/String;

    iget-object v1, p0, Lcom/androlua/LuaTimerTask;->mArg:[Ljava/lang/Object;

    invoke-direct {p0, v0, v1}, Lcom/androlua/LuaTimerTask;->newLuaThread(Ljava/lang/String;[Ljava/lang/Object;)V

    goto :goto_0

    :cond_2
    invoke-virtual {v1, v0}, Lcom/luajava/LuaState;->p(Ljava/lang/String;)I

    iget-object v1, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const/4 v2, -0x1

    invoke-virtual {v1, v2}, Lcom/luajava/LuaState;->F(I)Z

    move-result v1

    if-nez v1, :cond_3

    const/4 v1, 0x0

    new-array v1, v1, [Ljava/lang/Object;

    invoke-direct {p0, v0, v1}, Lcom/androlua/LuaTimerTask;->runFunc(Ljava/lang/String;[Ljava/lang/Object;)V

    goto :goto_0

    :cond_3
    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->mBuffer:[B

    if-eqz v0, :cond_4

    iget-object v1, p0, Lcom/androlua/LuaTimerTask;->mArg:[Ljava/lang/Object;

    invoke-direct {p0, v0, v1}, Lcom/androlua/LuaTimerTask;->newLuaThread([B[Ljava/lang/Object;)V

    goto :goto_0

    :cond_4
    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->mSrc:Ljava/lang/String;

    iget-object v1, p0, Lcom/androlua/LuaTimerTask;->mArg:[Ljava/lang/Object;

    invoke-direct {p0, v0, v1}, Lcom/androlua/LuaTimerTask;->newLuaThread(Ljava/lang/String;[Ljava/lang/Object;)V
    :try_end_0
    .catch Lcom/luajava/LuaException; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_0

    :catch_0
    move-exception v0

    iget-object v1, p0, Lcom/androlua/LuaTimerTask;->mLuaContext:Lcom/androlua/LuaContext;

    invoke-virtual {p0}, Ljava/lang/Object;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-interface {v1, v2, v0}, Lcom/androlua/LuaContext;->sendError(Ljava/lang/String;Ljava/lang/Exception;)V

    :goto_0
    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    const/4 v1, 0x2

    const/4 v2, 0x1

    invoke-virtual {v0, v1, v2}, Lcom/luajava/LuaState;->k(II)I

    invoke-static {}, Ljava/lang/System;->gc()V

    return-void
.end method

.method public set(Ljava/lang/String;Ljava/lang/Object;)V
    .locals 1

    iget-object v0, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    invoke-virtual {v0, p2}, Lcom/luajava/LuaState;->a0(Ljava/lang/Object;)V

    iget-object p2, p0, Lcom/androlua/LuaTimerTask;->L:Lcom/luajava/LuaState;

    invoke-virtual {p2, p1}, Lcom/luajava/LuaState;->m0(Ljava/lang/String;)V

    return-void
.end method

.method public setArg(Lcom/luajava/LuaObject;)V
    .locals 0

    invoke-virtual {p1}, Lcom/luajava/LuaObject;->asArray()[Ljava/lang/Object;

    move-result-object p1

    iput-object p1, p0, Lcom/androlua/LuaTimerTask;->mArg:[Ljava/lang/Object;

    return-void
.end method

.method public setArg([Ljava/lang/Object;)V
    .locals 0

    iput-object p1, p0, Lcom/androlua/LuaTimerTask;->mArg:[Ljava/lang/Object;

    return-void
.end method

.method public setEnabled(Z)V
    .locals 0

    iput-boolean p1, p0, Lcom/androlua/LuaTimerTask;->mEnabled:Z

    return-void
.end method
