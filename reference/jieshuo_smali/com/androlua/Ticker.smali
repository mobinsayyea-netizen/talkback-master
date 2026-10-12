.class public Lcom/androlua/Ticker;
.super Ljava/lang/Object;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lcom/androlua/Ticker$OnTickListener;
    }
.end annotation


# instance fields
.field private isRun:Z

.field private mEnabled:Z

.field private mHandler:Landroid/os/Handler;

.field private mLast:J

.field private mOffset:J

.field private mOnTickListener:Lcom/androlua/Ticker$OnTickListener;

.field private mPeriod:J

.field private mThread:Ljava/lang/Thread;


# direct methods
.method public constructor <init>()V
    .locals 2

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    const-wide/16 v0, 0x3e8

    iput-wide v0, p0, Lcom/androlua/Ticker;->mPeriod:J

    const/4 v0, 0x1

    iput-boolean v0, p0, Lcom/androlua/Ticker;->mEnabled:Z

    const/4 v0, 0x0

    iput-boolean v0, p0, Lcom/androlua/Ticker;->isRun:Z

    invoke-direct {p0}, Lcom/androlua/Ticker;->init()V

    return-void
.end method

.method static bridge synthetic a(Lcom/androlua/Ticker;)Z
    .locals 0

    iget-boolean p0, p0, Lcom/androlua/Ticker;->isRun:Z

    return p0
.end method

.method static bridge synthetic b(Lcom/androlua/Ticker;)Z
    .locals 0

    iget-boolean p0, p0, Lcom/androlua/Ticker;->mEnabled:Z

    return p0
.end method

.method static bridge synthetic c(Lcom/androlua/Ticker;)Landroid/os/Handler;
    .locals 0

    iget-object p0, p0, Lcom/androlua/Ticker;->mHandler:Landroid/os/Handler;

    return-object p0
.end method

.method static bridge synthetic d(Lcom/androlua/Ticker;)J
    .locals 2

    iget-wide v0, p0, Lcom/androlua/Ticker;->mLast:J

    return-wide v0
.end method

.method static bridge synthetic e(Lcom/androlua/Ticker;)J
    .locals 2

    iget-wide v0, p0, Lcom/androlua/Ticker;->mOffset:J

    return-wide v0
.end method

.method static bridge synthetic f(Lcom/androlua/Ticker;)Lcom/androlua/Ticker$OnTickListener;
    .locals 0

    iget-object p0, p0, Lcom/androlua/Ticker;->mOnTickListener:Lcom/androlua/Ticker$OnTickListener;

    return-object p0
.end method

.method static bridge synthetic g(Lcom/androlua/Ticker;)J
    .locals 2

    iget-wide v0, p0, Lcom/androlua/Ticker;->mPeriod:J

    return-wide v0
.end method

.method static bridge synthetic h(Lcom/androlua/Ticker;Z)V
    .locals 0

    iput-boolean p1, p0, Lcom/androlua/Ticker;->isRun:Z

    return-void
.end method

.method static bridge synthetic i(Lcom/androlua/Ticker;J)V
    .locals 0

    iput-wide p1, p0, Lcom/androlua/Ticker;->mLast:J

    return-void
.end method

.method private init()V
    .locals 1

    new-instance v0, Lcom/androlua/Ticker$1;

    invoke-direct {v0, p0}, Lcom/androlua/Ticker$1;-><init>(Lcom/androlua/Ticker;)V

    iput-object v0, p0, Lcom/androlua/Ticker;->mHandler:Landroid/os/Handler;

    new-instance v0, Lcom/androlua/Ticker$2;

    invoke-direct {v0, p0}, Lcom/androlua/Ticker$2;-><init>(Lcom/androlua/Ticker;)V

    iput-object v0, p0, Lcom/androlua/Ticker;->mThread:Ljava/lang/Thread;

    return-void
.end method


# virtual methods
.method public getEnabled()Z
    .locals 1

    iget-boolean v0, p0, Lcom/androlua/Ticker;->mEnabled:Z

    return v0
.end method

.method public getInterval()J
    .locals 2

    iget-wide v0, p0, Lcom/androlua/Ticker;->mPeriod:J

    return-wide v0
.end method

.method public getPeriod()J
    .locals 2

    iget-wide v0, p0, Lcom/androlua/Ticker;->mPeriod:J

    return-wide v0
.end method

.method public isRun()Z
    .locals 1

    iget-boolean v0, p0, Lcom/androlua/Ticker;->isRun:Z

    return v0
.end method

.method public setEnabled(Z)V
    .locals 4

    iput-boolean p1, p0, Lcom/androlua/Ticker;->mEnabled:Z

    if-nez p1, :cond_0

    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    move-result-wide v0

    iget-wide v2, p0, Lcom/androlua/Ticker;->mLast:J

    sub-long/2addr v0, v2

    iput-wide v0, p0, Lcom/androlua/Ticker;->mOffset:J

    :cond_0
    return-void
.end method

.method public setInterval(J)V
    .locals 2

    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    move-result-wide v0

    iput-wide v0, p0, Lcom/androlua/Ticker;->mLast:J

    iput-wide p1, p0, Lcom/androlua/Ticker;->mPeriod:J

    return-void
.end method

.method public setOnTickListener(Lcom/androlua/Ticker$OnTickListener;)V
    .locals 0

    iput-object p1, p0, Lcom/androlua/Ticker;->mOnTickListener:Lcom/androlua/Ticker$OnTickListener;

    return-void
.end method

.method public setPeriod(J)V
    .locals 2

    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    move-result-wide v0

    iput-wide v0, p0, Lcom/androlua/Ticker;->mLast:J

    iput-wide p1, p0, Lcom/androlua/Ticker;->mPeriod:J

    return-void
.end method

.method public start()V
    .locals 1

    iget-object v0, p0, Lcom/androlua/Ticker;->mThread:Ljava/lang/Thread;

    invoke-virtual {v0}, Ljava/lang/Thread;->start()V

    return-void
.end method

.method public stop()V
    .locals 1

    const/4 v0, 0x0

    iput-boolean v0, p0, Lcom/androlua/Ticker;->isRun:Z

    return-void
.end method
