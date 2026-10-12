.class public Lcom/androlua/LuaTimer;
.super Lcom/androlua/util/TimerX;
.source "SourceFile"

# interfaces
.implements Lcom/androlua/LuaGcable;


# instance fields
.field private task:Lcom/androlua/util/TimerTaskX;


# direct methods
.method public constructor <init>(Lcom/androlua/LuaContext;Lcom/luajava/LuaObject;)V
    .locals 1

    const/4 v0, 0x0

    invoke-direct {p0, p1, p2, v0}, Lcom/androlua/LuaTimer;-><init>(Lcom/androlua/LuaContext;Lcom/luajava/LuaObject;[Ljava/lang/Object;)V

    return-void
.end method

.method public constructor <init>(Lcom/androlua/LuaContext;Lcom/luajava/LuaObject;[Ljava/lang/Object;)V
    .locals 1

    const-string v0, "LuaTimer"

    invoke-direct {p0, v0}, Lcom/androlua/util/TimerX;-><init>(Ljava/lang/String;)V

    invoke-interface {p1, p0}, Lcom/androlua/LuaContext;->regGc(Lcom/androlua/LuaGcable;)V

    new-instance v0, Lcom/androlua/LuaTimerTask;

    invoke-direct {v0, p1, p2, p3}, Lcom/androlua/LuaTimerTask;-><init>(Lcom/androlua/LuaContext;Lcom/luajava/LuaObject;[Ljava/lang/Object;)V

    iput-object v0, p0, Lcom/androlua/LuaTimer;->task:Lcom/androlua/util/TimerTaskX;

    return-void
.end method

.method public constructor <init>(Lcom/androlua/LuaContext;Ljava/lang/String;)V
    .locals 1

    const/4 v0, 0x0

    invoke-direct {p0, p1, p2, v0}, Lcom/androlua/LuaTimer;-><init>(Lcom/androlua/LuaContext;Ljava/lang/String;[Ljava/lang/Object;)V

    return-void
.end method

.method public constructor <init>(Lcom/androlua/LuaContext;Ljava/lang/String;[Ljava/lang/Object;)V
    .locals 1

    const-string v0, "LuaTimer"

    invoke-direct {p0, v0}, Lcom/androlua/util/TimerX;-><init>(Ljava/lang/String;)V

    invoke-interface {p1, p0}, Lcom/androlua/LuaContext;->regGc(Lcom/androlua/LuaGcable;)V

    new-instance v0, Lcom/androlua/LuaTimerTask;

    invoke-direct {v0, p1, p2, p3}, Lcom/androlua/LuaTimerTask;-><init>(Lcom/androlua/LuaContext;Ljava/lang/String;[Ljava/lang/Object;)V

    iput-object v0, p0, Lcom/androlua/LuaTimer;->task:Lcom/androlua/util/TimerTaskX;

    return-void
.end method

.method public constructor <init>(Lcom/androlua/util/TimerTaskX;)V
    .locals 0

    invoke-direct {p0}, Lcom/androlua/util/TimerX;-><init>()V

    iput-object p1, p0, Lcom/androlua/LuaTimer;->task:Lcom/androlua/util/TimerTaskX;

    return-void
.end method


# virtual methods
.method public gc()V
    .locals 0

    invoke-virtual {p0}, Lcom/androlua/LuaTimer;->stop()V

    return-void
.end method

.method public getEnabled()Z
    .locals 1

    iget-object v0, p0, Lcom/androlua/LuaTimer;->task:Lcom/androlua/util/TimerTaskX;

    invoke-virtual {v0}, Lcom/androlua/util/TimerTaskX;->isEnabled()Z

    move-result v0

    return v0
.end method

.method public getPeriod()J
    .locals 2

    iget-object v0, p0, Lcom/androlua/LuaTimer;->task:Lcom/androlua/util/TimerTaskX;

    invoke-virtual {v0}, Lcom/androlua/util/TimerTaskX;->getPeriod()J

    move-result-wide v0

    return-wide v0
.end method

.method public isEnabled()Z
    .locals 1

    iget-object v0, p0, Lcom/androlua/LuaTimer;->task:Lcom/androlua/util/TimerTaskX;

    invoke-virtual {v0}, Lcom/androlua/util/TimerTaskX;->isEnabled()Z

    move-result v0

    return v0
.end method

.method public setEnabled(Z)V
    .locals 1

    iget-object v0, p0, Lcom/androlua/LuaTimer;->task:Lcom/androlua/util/TimerTaskX;

    invoke-virtual {v0, p1}, Lcom/androlua/util/TimerTaskX;->setEnabled(Z)V

    return-void
.end method

.method public setPeriod(J)V
    .locals 1

    iget-object v0, p0, Lcom/androlua/LuaTimer;->task:Lcom/androlua/util/TimerTaskX;

    invoke-virtual {v0, p1, p2}, Lcom/androlua/util/TimerTaskX;->setPeriod(J)V

    return-void
.end method

.method public start(J)V
    .locals 6

    iget-object v1, p0, Lcom/androlua/LuaTimer;->task:Lcom/androlua/util/TimerTaskX;

    const-wide/16 v4, 0x0

    move-object v0, p0

    move-wide v2, p1

    invoke-virtual/range {v0 .. v5}, Lcom/androlua/util/TimerX;->scheduleAtFixedRate(Lcom/androlua/util/TimerTaskX;JJ)V

    return-void
.end method

.method public start(JJ)V
    .locals 6

    iget-object v1, p0, Lcom/androlua/LuaTimer;->task:Lcom/androlua/util/TimerTaskX;

    move-object v0, p0

    move-wide v2, p1

    move-wide v4, p3

    invoke-virtual/range {v0 .. v5}, Lcom/androlua/util/TimerX;->scheduleAtFixedRate(Lcom/androlua/util/TimerTaskX;JJ)V

    return-void
.end method

.method public stop()V
    .locals 1

    iget-object v0, p0, Lcom/androlua/LuaTimer;->task:Lcom/androlua/util/TimerTaskX;

    invoke-virtual {v0}, Lcom/androlua/util/TimerTaskX;->cancel()Z

    return-void
.end method
