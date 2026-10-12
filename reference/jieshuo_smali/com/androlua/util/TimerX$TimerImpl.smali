.class final Lcom/androlua/util/TimerX$TimerImpl;
.super Ljava/lang/Thread;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lcom/androlua/util/TimerX;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x1a
    name = "TimerImpl"
.end annotation

.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;
    }
.end annotation


# instance fields
.field private cancelled:Z

.field private finished:Z

.field private tasks:Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;


# direct methods
.method constructor <init>(Ljava/lang/String;Z)V
    .locals 2

    invoke-direct {p0}, Ljava/lang/Thread;-><init>()V

    new-instance v0, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;

    const/4 v1, 0x0

    invoke-direct {v0, v1}, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;-><init>(Lcom/androlua/util/c;)V

    iput-object v0, p0, Lcom/androlua/util/TimerX$TimerImpl;->tasks:Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;

    invoke-virtual {p0, p1}, Ljava/lang/Thread;->setName(Ljava/lang/String;)V

    invoke-virtual {p0, p2}, Ljava/lang/Thread;->setDaemon(Z)V

    invoke-virtual {p0}, Ljava/lang/Thread;->start()V

    return-void
.end method

.method static bridge synthetic a(Lcom/androlua/util/TimerX$TimerImpl;)Z
    .locals 0

    iget-boolean p0, p0, Lcom/androlua/util/TimerX$TimerImpl;->cancelled:Z

    return p0
.end method

.method static bridge synthetic b(Lcom/androlua/util/TimerX$TimerImpl;Z)V
    .locals 0

    iput-boolean p1, p0, Lcom/androlua/util/TimerX$TimerImpl;->finished:Z

    return-void
.end method

.method static bridge synthetic c(Lcom/androlua/util/TimerX$TimerImpl;Lcom/androlua/util/TimerTaskX;)V
    .locals 0

    invoke-direct {p0, p1}, Lcom/androlua/util/TimerX$TimerImpl;->insertTask(Lcom/androlua/util/TimerTaskX;)V

    return-void
.end method

.method private insertTask(Lcom/androlua/util/TimerTaskX;)V
    .locals 1

    iget-object v0, p0, Lcom/androlua/util/TimerX$TimerImpl;->tasks:Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;

    invoke-virtual {v0, p1}, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->insert(Lcom/androlua/util/TimerTaskX;)V

    invoke-virtual {p0}, Ljava/lang/Object;->notify()V

    return-void
.end method


# virtual methods
.method public declared-synchronized cancel()V
    .locals 1

    monitor-enter p0

    const/4 v0, 0x1

    :try_start_0
    iput-boolean v0, p0, Lcom/androlua/util/TimerX$TimerImpl;->cancelled:Z

    iget-object v0, p0, Lcom/androlua/util/TimerX$TimerImpl;->tasks:Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;

    invoke-virtual {v0}, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->reset()V

    invoke-virtual {p0}, Ljava/lang/Object;->notify()V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    monitor-exit p0

    return-void

    :catchall_0
    move-exception v0

    monitor-exit p0

    throw v0
.end method

.method public purge()I
    .locals 2

    iget-object v0, p0, Lcom/androlua/util/TimerX$TimerImpl;->tasks:Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;

    invoke-virtual {v0}, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->isEmpty()Z

    move-result v0

    const/4 v1, 0x0

    if-eqz v0, :cond_0

    return v1

    :cond_0
    iget-object v0, p0, Lcom/androlua/util/TimerX$TimerImpl;->tasks:Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;

    invoke-static {v0, v1}, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->b(Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;I)V

    iget-object v0, p0, Lcom/androlua/util/TimerX$TimerImpl;->tasks:Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;

    invoke-virtual {v0}, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->deleteIfCancelled()V

    iget-object v0, p0, Lcom/androlua/util/TimerX$TimerImpl;->tasks:Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;

    invoke-static {v0}, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->a(Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;)I

    move-result v0

    return v0
.end method

.method public run()V
    .locals 10

    const-string v0, "TimerX"

    invoke-virtual {p0, v0}, Ljava/lang/Thread;->setName(Ljava/lang/String;)V

    :cond_0
    :goto_0
    monitor-enter p0

    :try_start_0
    iget-boolean v0, p0, Lcom/androlua/util/TimerX$TimerImpl;->cancelled:Z

    if-eqz v0, :cond_1

    monitor-exit p0

    return-void

    :cond_1
    iget-object v0, p0, Lcom/androlua/util/TimerX$TimerImpl;->tasks:Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;

    invoke-virtual {v0}, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->isEmpty()Z

    move-result v0

    if-eqz v0, :cond_3

    iget-boolean v0, p0, Lcom/androlua/util/TimerX$TimerImpl;->finished:Z

    if-eqz v0, :cond_2

    monitor-exit p0
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_4

    return-void

    :cond_2
    :try_start_1
    invoke-virtual {p0}, Ljava/lang/Object;->wait()V
    :try_end_1
    .catch Ljava/lang/InterruptedException; {:try_start_1 .. :try_end_1} :catch_0
    .catchall {:try_start_1 .. :try_end_1} :catchall_4

    :catch_0
    :goto_1
    :try_start_2
    monitor-exit p0

    goto :goto_0

    :cond_3
    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    move-result-wide v0

    iget-object v2, p0, Lcom/androlua/util/TimerX$TimerImpl;->tasks:Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;

    invoke-virtual {v2}, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->minimum()Lcom/androlua/util/TimerTaskX;

    move-result-object v2

    iget-object v3, v2, Lcom/androlua/util/TimerTaskX;->lock:Ljava/lang/Object;

    monitor-enter v3
    :try_end_2
    .catchall {:try_start_2 .. :try_end_2} :catchall_4

    :try_start_3
    iget-boolean v4, v2, Lcom/androlua/util/TimerTaskX;->cancelled:Z

    const/4 v5, 0x0

    if-eqz v4, :cond_4

    iget-object v0, p0, Lcom/androlua/util/TimerX$TimerImpl;->tasks:Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;

    invoke-virtual {v0, v5}, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->delete(I)V

    monitor-exit v3

    goto :goto_1

    :cond_4
    iget-wide v6, v2, Lcom/androlua/util/TimerTaskX;->when:J

    sub-long/2addr v6, v0

    monitor-exit v3
    :try_end_3
    .catchall {:try_start_3 .. :try_end_3} :catchall_3

    const-wide/16 v0, 0x0

    cmp-long v3, v6, v0

    if-lez v3, :cond_5

    :try_start_4
    invoke-virtual {p0, v6, v7}, Ljava/lang/Object;->wait(J)V
    :try_end_4
    .catch Ljava/lang/InterruptedException; {:try_start_4 .. :try_end_4} :catch_0
    .catchall {:try_start_4 .. :try_end_4} :catchall_4

    goto :goto_1

    :cond_5
    :try_start_5
    iget-object v3, v2, Lcom/androlua/util/TimerTaskX;->lock:Ljava/lang/Object;

    monitor-enter v3
    :try_end_5
    .catchall {:try_start_5 .. :try_end_5} :catchall_4

    :try_start_6
    iget-object v4, p0, Lcom/androlua/util/TimerX$TimerImpl;->tasks:Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;

    invoke-virtual {v4}, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->minimum()Lcom/androlua/util/TimerTaskX;

    move-result-object v4

    iget-wide v6, v4, Lcom/androlua/util/TimerTaskX;->when:J

    iget-wide v8, v2, Lcom/androlua/util/TimerTaskX;->when:J

    cmp-long v4, v6, v8

    if-eqz v4, :cond_6

    iget-object v4, p0, Lcom/androlua/util/TimerX$TimerImpl;->tasks:Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;

    invoke-static {v4, v2}, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->c(Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;Lcom/androlua/util/TimerTaskX;)I

    move-result v5

    :cond_6
    iget-boolean v4, v2, Lcom/androlua/util/TimerTaskX;->cancelled:Z

    if-eqz v4, :cond_7

    iget-object v0, p0, Lcom/androlua/util/TimerX$TimerImpl;->tasks:Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;

    invoke-static {v0, v2}, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->c(Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;Lcom/androlua/util/TimerTaskX;)I

    move-result v1

    invoke-virtual {v0, v1}, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->delete(I)V

    monitor-exit v3

    goto :goto_1

    :cond_7
    iget-wide v6, v2, Lcom/androlua/util/TimerTaskX;->when:J

    invoke-virtual {v2, v6, v7}, Lcom/androlua/util/TimerTaskX;->setScheduledTime(J)V

    iget-object v4, p0, Lcom/androlua/util/TimerX$TimerImpl;->tasks:Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;

    invoke-virtual {v4, v5}, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->delete(I)V

    iget-wide v4, v2, Lcom/androlua/util/TimerTaskX;->period:J

    cmp-long v6, v4, v0

    if-ltz v6, :cond_9

    iget-boolean v0, v2, Lcom/androlua/util/TimerTaskX;->fixedRate:Z

    if-eqz v0, :cond_8

    iget-wide v0, v2, Lcom/androlua/util/TimerTaskX;->when:J

    :goto_2
    add-long/2addr v0, v4

    iput-wide v0, v2, Lcom/androlua/util/TimerTaskX;->when:J

    goto :goto_3

    :cond_8
    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    move-result-wide v0

    iget-wide v4, v2, Lcom/androlua/util/TimerTaskX;->period:J

    goto :goto_2

    :goto_3
    invoke-direct {p0, v2}, Lcom/androlua/util/TimerX$TimerImpl;->insertTask(Lcom/androlua/util/TimerTaskX;)V

    goto :goto_4

    :cond_9
    iput-wide v0, v2, Lcom/androlua/util/TimerTaskX;->when:J

    :goto_4
    monitor-exit v3
    :try_end_6
    .catchall {:try_start_6 .. :try_end_6} :catchall_2

    :try_start_7
    monitor-exit p0
    :try_end_7
    .catchall {:try_start_7 .. :try_end_7} :catchall_4

    :try_start_8
    invoke-virtual {v2}, Lcom/androlua/util/TimerTaskX;->isEnabled()Z

    move-result v0

    if-eqz v0, :cond_0

    invoke-virtual {v2}, Lcom/androlua/util/TimerTaskX;->run()V
    :try_end_8
    .catchall {:try_start_8 .. :try_end_8} :catchall_0

    goto/16 :goto_0

    :catchall_0
    move-exception v0

    monitor-enter p0

    const/4 v1, 0x1

    :try_start_9
    iput-boolean v1, p0, Lcom/androlua/util/TimerX$TimerImpl;->cancelled:Z

    monitor-exit p0
    :try_end_9
    .catchall {:try_start_9 .. :try_end_9} :catchall_1

    throw v0

    :catchall_1
    move-exception v0

    :try_start_a
    monitor-exit p0
    :try_end_a
    .catchall {:try_start_a .. :try_end_a} :catchall_1

    throw v0

    :catchall_2
    move-exception v0

    :try_start_b
    monitor-exit v3
    :try_end_b
    .catchall {:try_start_b .. :try_end_b} :catchall_2

    :try_start_c
    throw v0
    :try_end_c
    .catchall {:try_start_c .. :try_end_c} :catchall_4

    :catchall_3
    move-exception v0

    :try_start_d
    monitor-exit v3
    :try_end_d
    .catchall {:try_start_d .. :try_end_d} :catchall_3

    :try_start_e
    throw v0

    :catchall_4
    move-exception v0

    monitor-exit p0
    :try_end_e
    .catchall {:try_start_e .. :try_end_e} :catchall_4

    throw v0
.end method
