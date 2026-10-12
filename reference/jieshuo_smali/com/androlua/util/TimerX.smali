.class public Lcom/androlua/util/TimerX;
.super Ljava/lang/Object;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lcom/androlua/util/TimerX$TimerImpl;,
        Lcom/androlua/util/TimerX$FinalizerHelper;
    }
.end annotation


# static fields
.field private static timerId:J


# instance fields
.field private final finalizer:Lcom/androlua/util/TimerX$FinalizerHelper;

.field private final impl:Lcom/androlua/util/TimerX$TimerImpl;


# direct methods
.method public constructor <init>()V
    .locals 1

    const/4 v0, 0x0

    invoke-direct {p0, v0}, Lcom/androlua/util/TimerX;-><init>(Z)V

    return-void
.end method

.method public constructor <init>(Ljava/lang/String;)V
    .locals 1

    const/4 v0, 0x0

    invoke-direct {p0, p1, v0}, Lcom/androlua/util/TimerX;-><init>(Ljava/lang/String;Z)V

    return-void
.end method

.method public constructor <init>(Ljava/lang/String;Z)V
    .locals 1

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    if-eqz p1, :cond_0

    new-instance v0, Lcom/androlua/util/TimerX$TimerImpl;

    invoke-direct {v0, p1, p2}, Lcom/androlua/util/TimerX$TimerImpl;-><init>(Ljava/lang/String;Z)V

    iput-object v0, p0, Lcom/androlua/util/TimerX;->impl:Lcom/androlua/util/TimerX$TimerImpl;

    new-instance p1, Lcom/androlua/util/TimerX$FinalizerHelper;

    invoke-direct {p1, v0}, Lcom/androlua/util/TimerX$FinalizerHelper;-><init>(Lcom/androlua/util/TimerX$TimerImpl;)V

    iput-object p1, p0, Lcom/androlua/util/TimerX;->finalizer:Lcom/androlua/util/TimerX$FinalizerHelper;

    return-void

    :cond_0
    new-instance p1, Ljava/lang/NullPointerException;

    const-string p2, "name is null"

    invoke-direct {p1, p2}, Ljava/lang/NullPointerException;-><init>(Ljava/lang/String;)V

    throw p1
.end method

.method public constructor <init>(Z)V
    .locals 3

    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    const-string v1, "Timer-"

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-static {}, Lcom/androlua/util/TimerX;->nextId()J

    move-result-wide v1

    invoke-virtual {v0, v1, v2}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-direct {p0, v0, p1}, Lcom/androlua/util/TimerX;-><init>(Ljava/lang/String;Z)V

    return-void
.end method

.method private static declared-synchronized nextId()J
    .locals 5

    const-class v0, Lcom/androlua/util/TimerX;

    monitor-enter v0

    :try_start_0
    sget-wide v1, Lcom/androlua/util/TimerX;->timerId:J

    const-wide/16 v3, 0x1

    add-long/2addr v3, v1

    sput-wide v3, Lcom/androlua/util/TimerX;->timerId:J
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    monitor-exit v0

    return-wide v1

    :catchall_0
    move-exception v1

    monitor-exit v0

    throw v1
.end method

.method private scheduleImpl(Lcom/androlua/util/TimerTaskX;JJZ)V
    .locals 3

    iget-object v0, p0, Lcom/androlua/util/TimerX;->impl:Lcom/androlua/util/TimerX$TimerImpl;

    monitor-enter v0

    :try_start_0
    iget-object v1, p0, Lcom/androlua/util/TimerX;->impl:Lcom/androlua/util/TimerX$TimerImpl;

    invoke-static {v1}, Lcom/androlua/util/TimerX$TimerImpl;->a(Lcom/androlua/util/TimerX$TimerImpl;)Z

    move-result v1

    if-nez v1, :cond_3

    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    move-result-wide v1

    add-long/2addr p2, v1

    const-wide/16 v1, 0x0

    cmp-long v1, p2, v1

    if-ltz v1, :cond_2

    iget-object v1, p1, Lcom/androlua/util/TimerTaskX;->lock:Ljava/lang/Object;

    monitor-enter v1
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_1

    :try_start_1
    invoke-virtual {p1}, Lcom/androlua/util/TimerTaskX;->isScheduled()Z

    move-result v2

    if-nez v2, :cond_1

    iget-boolean v2, p1, Lcom/androlua/util/TimerTaskX;->cancelled:Z

    if-nez v2, :cond_0

    iput-wide p2, p1, Lcom/androlua/util/TimerTaskX;->when:J

    iput-wide p4, p1, Lcom/androlua/util/TimerTaskX;->period:J

    iput-boolean p6, p1, Lcom/androlua/util/TimerTaskX;->fixedRate:Z

    monitor-exit v1
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    :try_start_2
    iget-object p2, p0, Lcom/androlua/util/TimerX;->impl:Lcom/androlua/util/TimerX$TimerImpl;

    invoke-static {p2, p1}, Lcom/androlua/util/TimerX$TimerImpl;->c(Lcom/androlua/util/TimerX$TimerImpl;Lcom/androlua/util/TimerTaskX;)V

    monitor-exit v0
    :try_end_2
    .catchall {:try_start_2 .. :try_end_2} :catchall_1

    return-void

    :cond_0
    :try_start_3
    new-instance p1, Ljava/lang/IllegalStateException;

    const-string p2, "TimerTask is canceled"

    invoke-direct {p1, p2}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw p1

    :cond_1
    new-instance p1, Ljava/lang/IllegalStateException;

    const-string p2, "TimerTask is scheduled already"

    invoke-direct {p1, p2}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw p1

    :catchall_0
    move-exception p1

    monitor-exit v1
    :try_end_3
    .catchall {:try_start_3 .. :try_end_3} :catchall_0

    :try_start_4
    throw p1

    :cond_2
    new-instance p1, Ljava/lang/IllegalArgumentException;

    new-instance p4, Ljava/lang/StringBuilder;

    invoke-direct {p4}, Ljava/lang/StringBuilder;-><init>()V

    const-string p5, "Illegal delay to start the TimerTask: "

    invoke-virtual {p4, p5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p4, p2, p3}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;

    invoke-virtual {p4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p2

    invoke-direct {p1, p2}, Ljava/lang/IllegalArgumentException;-><init>(Ljava/lang/String;)V

    throw p1

    :cond_3
    new-instance p1, Ljava/lang/IllegalStateException;

    const-string p2, "Timer was canceled"

    invoke-direct {p1, p2}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw p1

    :catchall_1
    move-exception p1

    monitor-exit v0
    :try_end_4
    .catchall {:try_start_4 .. :try_end_4} :catchall_1

    throw p1
.end method


# virtual methods
.method public cancel()V
    .locals 1

    iget-object v0, p0, Lcom/androlua/util/TimerX;->impl:Lcom/androlua/util/TimerX$TimerImpl;

    invoke-virtual {v0}, Lcom/androlua/util/TimerX$TimerImpl;->cancel()V

    return-void
.end method

.method public purge()I
    .locals 2

    iget-object v0, p0, Lcom/androlua/util/TimerX;->impl:Lcom/androlua/util/TimerX$TimerImpl;

    monitor-enter v0

    :try_start_0
    iget-object v1, p0, Lcom/androlua/util/TimerX;->impl:Lcom/androlua/util/TimerX$TimerImpl;

    invoke-virtual {v1}, Lcom/androlua/util/TimerX$TimerImpl;->purge()I

    move-result v1

    monitor-exit v0

    return v1

    :catchall_0
    move-exception v1

    monitor-exit v0
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    throw v1
.end method

.method public schedule(Lcom/androlua/util/TimerTaskX;J)V
    .locals 8

    const-wide/16 v0, 0x0

    cmp-long v0, p2, v0

    if-ltz v0, :cond_0

    const-wide/16 v5, -0x1

    const/4 v7, 0x0

    move-object v1, p0

    move-object v2, p1

    move-wide v3, p2

    invoke-direct/range {v1 .. v7}, Lcom/androlua/util/TimerX;->scheduleImpl(Lcom/androlua/util/TimerTaskX;JJZ)V

    return-void

    :cond_0
    new-instance p1, Ljava/lang/IllegalArgumentException;

    invoke-direct {p1}, Ljava/lang/IllegalArgumentException;-><init>()V

    throw p1
.end method

.method public schedule(Lcom/androlua/util/TimerTaskX;JJ)V
    .locals 8

    const-wide/16 v0, 0x0

    cmp-long v2, p2, v0

    if-ltz v2, :cond_0

    cmp-long v0, p4, v0

    if-lez v0, :cond_0

    const/4 v7, 0x0

    move-object v1, p0

    move-object v2, p1

    move-wide v3, p2

    move-wide v5, p4

    invoke-direct/range {v1 .. v7}, Lcom/androlua/util/TimerX;->scheduleImpl(Lcom/androlua/util/TimerTaskX;JJZ)V

    return-void

    :cond_0
    new-instance p1, Ljava/lang/IllegalArgumentException;

    invoke-direct {p1}, Ljava/lang/IllegalArgumentException;-><init>()V

    throw p1
.end method

.method public schedule(Lcom/androlua/util/TimerTaskX;Ljava/util/Date;)V
    .locals 11

    invoke-virtual {p2}, Ljava/util/Date;->getTime()J

    move-result-wide v0

    const-wide/16 v2, 0x0

    cmp-long v0, v0, v2

    if-ltz v0, :cond_1

    invoke-virtual {p2}, Ljava/util/Date;->getTime()J

    move-result-wide v0

    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    move-result-wide v4

    sub-long/2addr v0, v4

    cmp-long p2, v0, v2

    if-gez p2, :cond_0

    move-wide v6, v2

    goto :goto_0

    :cond_0
    move-wide v6, v0

    :goto_0
    const-wide/16 v8, -0x1

    const/4 v10, 0x0

    move-object v4, p0

    move-object v5, p1

    invoke-direct/range {v4 .. v10}, Lcom/androlua/util/TimerX;->scheduleImpl(Lcom/androlua/util/TimerTaskX;JJZ)V

    return-void

    :cond_1
    new-instance p1, Ljava/lang/IllegalArgumentException;

    invoke-direct {p1}, Ljava/lang/IllegalArgumentException;-><init>()V

    throw p1
.end method

.method public schedule(Lcom/androlua/util/TimerTaskX;Ljava/util/Date;J)V
    .locals 11

    const-wide/16 v0, 0x0

    cmp-long v2, p3, v0

    if-lez v2, :cond_1

    invoke-virtual {p2}, Ljava/util/Date;->getTime()J

    move-result-wide v2

    cmp-long v2, v2, v0

    if-ltz v2, :cond_1

    invoke-virtual {p2}, Ljava/util/Date;->getTime()J

    move-result-wide v2

    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    move-result-wide v4

    sub-long/2addr v2, v4

    cmp-long p2, v2, v0

    if-gez p2, :cond_0

    move-wide v6, v0

    goto :goto_0

    :cond_0
    move-wide v6, v2

    :goto_0
    const/4 v10, 0x0

    move-object v4, p0

    move-object v5, p1

    move-wide v8, p3

    invoke-direct/range {v4 .. v10}, Lcom/androlua/util/TimerX;->scheduleImpl(Lcom/androlua/util/TimerTaskX;JJZ)V

    return-void

    :cond_1
    new-instance p1, Ljava/lang/IllegalArgumentException;

    invoke-direct {p1}, Ljava/lang/IllegalArgumentException;-><init>()V

    throw p1
.end method

.method public scheduleAtFixedRate(Lcom/androlua/util/TimerTaskX;JJ)V
    .locals 8

    const-wide/16 v0, 0x0

    cmp-long v2, p2, v0

    if-ltz v2, :cond_0

    cmp-long v0, p4, v0

    if-lez v0, :cond_0

    const/4 v7, 0x1

    move-object v1, p0

    move-object v2, p1

    move-wide v3, p2

    move-wide v5, p4

    invoke-direct/range {v1 .. v7}, Lcom/androlua/util/TimerX;->scheduleImpl(Lcom/androlua/util/TimerTaskX;JJZ)V

    return-void

    :cond_0
    new-instance p1, Ljava/lang/IllegalArgumentException;

    invoke-direct {p1}, Ljava/lang/IllegalArgumentException;-><init>()V

    throw p1
.end method

.method public scheduleAtFixedRate(Lcom/androlua/util/TimerTaskX;Ljava/util/Date;J)V
    .locals 11

    const-wide/16 v0, 0x0

    cmp-long v2, p3, v0

    if-lez v2, :cond_0

    invoke-virtual {p2}, Ljava/util/Date;->getTime()J

    move-result-wide v2

    cmp-long v0, v2, v0

    if-ltz v0, :cond_0

    invoke-virtual {p2}, Ljava/util/Date;->getTime()J

    move-result-wide v0

    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    move-result-wide v2

    sub-long v6, v0, v2

    const/4 v10, 0x1

    move-object v4, p0

    move-object v5, p1

    move-wide v8, p3

    invoke-direct/range {v4 .. v10}, Lcom/androlua/util/TimerX;->scheduleImpl(Lcom/androlua/util/TimerTaskX;JJZ)V

    return-void

    :cond_0
    new-instance p1, Ljava/lang/IllegalArgumentException;

    invoke-direct {p1}, Ljava/lang/IllegalArgumentException;-><init>()V

    throw p1
.end method
