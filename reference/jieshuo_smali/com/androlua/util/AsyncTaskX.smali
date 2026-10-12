.class public abstract Lcom/androlua/util/AsyncTaskX;
.super Ljava/lang/Object;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lcom/androlua/util/AsyncTaskX$InternalHandler;,
        Lcom/androlua/util/AsyncTaskX$Status;,
        Lcom/androlua/util/AsyncTaskX$WorkerRunnable;,
        Lcom/androlua/util/AsyncTaskX$AsyncTaskResult;,
        Lcom/androlua/util/AsyncTaskX$SerialExecutor;
    }
.end annotation

.annotation system Ldalvik/annotation/Signature;
    value = {
        "<Params:",
        "Ljava/lang/Object;",
        "Progress:",
        "Ljava/lang/Object;",
        "Result:",
        "Ljava/lang/Object;",
        ">",
        "Ljava/lang/Object;"
    }
.end annotation


# static fields
.field private static final CORE_POOL_SIZE:I = 0x400

.field private static final CPU_COUNT:I

.field private static final KEEP_ALIVE_SECONDS:I = 0x1e

.field private static final LOG_TAG:Ljava/lang/String; = "AsyncTaskX"

.field private static final MAXIMUM_POOL_SIZE:I = 0x400

.field private static final MESSAGE_POST_PROGRESS:I = 0x2

.field private static final MESSAGE_POST_RESULT:I = 0x1

.field public static final SERIAL_EXECUTOR:Ljava/util/concurrent/Executor;

.field public static final THREAD_POOL_EXECUTOR:Ljava/util/concurrent/Executor;

.field private static idx:I

.field private static volatile sDefaultExecutor:Ljava/util/concurrent/Executor;

.field private static sHandler:Lcom/androlua/util/AsyncTaskX$InternalHandler;

.field private static final sPoolWorkQueue:Ljava/util/concurrent/BlockingQueue;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/concurrent/BlockingQueue<",
            "Ljava/lang/Runnable;",
            ">;"
        }
    .end annotation
.end field

.field private static final sThreadFactory:Ljava/util/concurrent/ThreadFactory;


# instance fields
.field private final mCancelled:Ljava/util/concurrent/atomic/AtomicBoolean;

.field private final mFuture:Ljava/util/concurrent/FutureTask;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/concurrent/FutureTask<",
            "TResult;>;"
        }
    .end annotation
.end field

.field private final mHandler:Landroid/os/Handler;

.field private volatile mStatus:Lcom/androlua/util/AsyncTaskX$Status;

.field private final mTaskInvoked:Ljava/util/concurrent/atomic/AtomicBoolean;

.field private final mWorker:Lcom/androlua/util/AsyncTaskX$WorkerRunnable;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Lcom/androlua/util/AsyncTaskX$WorkerRunnable<",
            "TParams;TResult;>;"
        }
    .end annotation
.end field


# direct methods
.method static constructor <clinit>()V
    .locals 9

    invoke-static {}, Ljava/lang/Runtime;->getRuntime()Ljava/lang/Runtime;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/Runtime;->availableProcessors()I

    move-result v0

    sput v0, Lcom/androlua/util/AsyncTaskX;->CPU_COUNT:I

    new-instance v8, Lcom/androlua/util/AsyncTaskX$1;

    invoke-direct {v8}, Lcom/androlua/util/AsyncTaskX$1;-><init>()V

    sput-object v8, Lcom/androlua/util/AsyncTaskX;->sThreadFactory:Ljava/util/concurrent/ThreadFactory;

    new-instance v7, Ljava/util/concurrent/LinkedBlockingQueue;

    const/16 v0, 0x2000

    invoke-direct {v7, v0}, Ljava/util/concurrent/LinkedBlockingQueue;-><init>(I)V

    sput-object v7, Lcom/androlua/util/AsyncTaskX;->sPoolWorkQueue:Ljava/util/concurrent/BlockingQueue;

    new-instance v0, Ljava/util/concurrent/ThreadPoolExecutor;

    const/16 v2, 0x400

    const/16 v3, 0x400

    const-wide/16 v4, 0x1e

    sget-object v6, Ljava/util/concurrent/TimeUnit;->SECONDS:Ljava/util/concurrent/TimeUnit;

    move-object v1, v0

    invoke-direct/range {v1 .. v8}, Ljava/util/concurrent/ThreadPoolExecutor;-><init>(IIJLjava/util/concurrent/TimeUnit;Ljava/util/concurrent/BlockingQueue;Ljava/util/concurrent/ThreadFactory;)V

    const/4 v1, 0x1

    invoke-virtual {v0, v1}, Ljava/util/concurrent/ThreadPoolExecutor;->allowCoreThreadTimeOut(Z)V

    sput-object v0, Lcom/androlua/util/AsyncTaskX;->THREAD_POOL_EXECUTOR:Ljava/util/concurrent/Executor;

    new-instance v1, Lcom/androlua/util/AsyncTaskX$SerialExecutor;

    const/4 v2, 0x0

    invoke-direct {v1, v2}, Lcom/androlua/util/AsyncTaskX$SerialExecutor;-><init>(Lcom/androlua/util/a;)V

    sput-object v1, Lcom/androlua/util/AsyncTaskX;->SERIAL_EXECUTOR:Ljava/util/concurrent/Executor;

    sput-object v0, Lcom/androlua/util/AsyncTaskX;->sDefaultExecutor:Ljava/util/concurrent/Executor;

    const/4 v0, 0x0

    sput v0, Lcom/androlua/util/AsyncTaskX;->idx:I

    return-void
.end method

.method public constructor <init>()V
    .locals 1

    const/4 v0, 0x0

    invoke-direct {p0, v0}, Lcom/androlua/util/AsyncTaskX;-><init>(Landroid/os/Looper;)V

    return-void
.end method

.method public constructor <init>(Landroid/os/Handler;)V
    .locals 0

    if-eqz p1, :cond_0

    invoke-virtual {p1}, Landroid/os/Handler;->getLooper()Landroid/os/Looper;

    move-result-object p1

    goto :goto_0

    :cond_0
    const/4 p1, 0x0

    :goto_0
    invoke-direct {p0, p1}, Lcom/androlua/util/AsyncTaskX;-><init>(Landroid/os/Looper;)V

    return-void
.end method

.method public constructor <init>(Landroid/os/Looper;)V
    .locals 1

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    sget-object v0, Lcom/androlua/util/AsyncTaskX$Status;->PENDING:Lcom/androlua/util/AsyncTaskX$Status;

    iput-object v0, p0, Lcom/androlua/util/AsyncTaskX;->mStatus:Lcom/androlua/util/AsyncTaskX$Status;

    new-instance v0, Ljava/util/concurrent/atomic/AtomicBoolean;

    invoke-direct {v0}, Ljava/util/concurrent/atomic/AtomicBoolean;-><init>()V

    iput-object v0, p0, Lcom/androlua/util/AsyncTaskX;->mCancelled:Ljava/util/concurrent/atomic/AtomicBoolean;

    new-instance v0, Ljava/util/concurrent/atomic/AtomicBoolean;

    invoke-direct {v0}, Ljava/util/concurrent/atomic/AtomicBoolean;-><init>()V

    iput-object v0, p0, Lcom/androlua/util/AsyncTaskX;->mTaskInvoked:Ljava/util/concurrent/atomic/AtomicBoolean;

    if-eqz p1, :cond_1

    invoke-static {}, Landroid/os/Looper;->getMainLooper()Landroid/os/Looper;

    move-result-object v0

    if-ne p1, v0, :cond_0

    goto :goto_0

    :cond_0
    new-instance v0, Landroid/os/Handler;

    invoke-direct {v0, p1}, Landroid/os/Handler;-><init>(Landroid/os/Looper;)V

    goto :goto_1

    :cond_1
    :goto_0
    invoke-static {}, Lcom/androlua/util/AsyncTaskX;->getMainHandler()Landroid/os/Handler;

    move-result-object v0

    :goto_1
    iput-object v0, p0, Lcom/androlua/util/AsyncTaskX;->mHandler:Landroid/os/Handler;

    new-instance p1, Lcom/androlua/util/AsyncTaskX$2;

    invoke-direct {p1, p0}, Lcom/androlua/util/AsyncTaskX$2;-><init>(Lcom/androlua/util/AsyncTaskX;)V

    iput-object p1, p0, Lcom/androlua/util/AsyncTaskX;->mWorker:Lcom/androlua/util/AsyncTaskX$WorkerRunnable;

    new-instance v0, Lcom/androlua/util/AsyncTaskX$3;

    invoke-direct {v0, p0, p1}, Lcom/androlua/util/AsyncTaskX$3;-><init>(Lcom/androlua/util/AsyncTaskX;Ljava/util/concurrent/Callable;)V

    iput-object v0, p0, Lcom/androlua/util/AsyncTaskX;->mFuture:Ljava/util/concurrent/FutureTask;

    return-void
.end method

.method static bridge synthetic a(Lcom/androlua/util/AsyncTaskX;)Ljava/util/concurrent/atomic/AtomicBoolean;
    .locals 0

    iget-object p0, p0, Lcom/androlua/util/AsyncTaskX;->mCancelled:Ljava/util/concurrent/atomic/AtomicBoolean;

    return-object p0
.end method

.method static bridge synthetic b(Lcom/androlua/util/AsyncTaskX;)Ljava/util/concurrent/atomic/AtomicBoolean;
    .locals 0

    iget-object p0, p0, Lcom/androlua/util/AsyncTaskX;->mTaskInvoked:Ljava/util/concurrent/atomic/AtomicBoolean;

    return-object p0
.end method

.method static bridge synthetic c(Lcom/androlua/util/AsyncTaskX;Ljava/lang/Object;)V
    .locals 0

    invoke-direct {p0, p1}, Lcom/androlua/util/AsyncTaskX;->finish(Ljava/lang/Object;)V

    return-void
.end method

.method static bridge synthetic d(Lcom/androlua/util/AsyncTaskX;Ljava/lang/Object;)Ljava/lang/Object;
    .locals 0

    invoke-direct {p0, p1}, Lcom/androlua/util/AsyncTaskX;->postResult(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object p0

    return-object p0
.end method

.method static bridge synthetic e(Lcom/androlua/util/AsyncTaskX;Ljava/lang/Object;)V
    .locals 0

    invoke-direct {p0, p1}, Lcom/androlua/util/AsyncTaskX;->postResultIfNotInvoked(Ljava/lang/Object;)V

    return-void
.end method

.method public static execute(Ljava/lang/Runnable;)V
    .locals 1

    sget-object v0, Lcom/androlua/util/AsyncTaskX;->sDefaultExecutor:Ljava/util/concurrent/Executor;

    invoke-interface {v0, p0}, Ljava/util/concurrent/Executor;->execute(Ljava/lang/Runnable;)V

    return-void
.end method

.method private finish(Ljava/lang/Object;)V
    .locals 1
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(TResult;)V"
        }
    .end annotation

    invoke-virtual {p0}, Lcom/androlua/util/AsyncTaskX;->isCancelled()Z

    move-result v0

    if-eqz v0, :cond_0

    invoke-virtual {p0, p1}, Lcom/androlua/util/AsyncTaskX;->onCancelled(Ljava/lang/Object;)V

    goto :goto_0

    :cond_0
    invoke-virtual {p0, p1}, Lcom/androlua/util/AsyncTaskX;->onPostExecute(Ljava/lang/Object;)V

    :goto_0
    sget-object p1, Lcom/androlua/util/AsyncTaskX$Status;->FINISHED:Lcom/androlua/util/AsyncTaskX$Status;

    iput-object p1, p0, Lcom/androlua/util/AsyncTaskX;->mStatus:Lcom/androlua/util/AsyncTaskX$Status;

    return-void
.end method

.method private getHandler()Landroid/os/Handler;
    .locals 1

    iget-object v0, p0, Lcom/androlua/util/AsyncTaskX;->mHandler:Landroid/os/Handler;

    return-object v0
.end method

.method private static getMainHandler()Landroid/os/Handler;
    .locals 3

    const-class v0, Lcom/androlua/util/AsyncTaskX;

    monitor-enter v0

    :try_start_0
    sget-object v1, Lcom/androlua/util/AsyncTaskX;->sHandler:Lcom/androlua/util/AsyncTaskX$InternalHandler;

    if-nez v1, :cond_0

    new-instance v1, Lcom/androlua/util/AsyncTaskX$InternalHandler;

    invoke-static {}, Landroid/os/Looper;->getMainLooper()Landroid/os/Looper;

    move-result-object v2

    invoke-direct {v1, v2}, Lcom/androlua/util/AsyncTaskX$InternalHandler;-><init>(Landroid/os/Looper;)V

    sput-object v1, Lcom/androlua/util/AsyncTaskX;->sHandler:Lcom/androlua/util/AsyncTaskX$InternalHandler;

    :cond_0
    sget-object v1, Lcom/androlua/util/AsyncTaskX;->sHandler:Lcom/androlua/util/AsyncTaskX$InternalHandler;

    monitor-exit v0

    return-object v1

    :catchall_0
    move-exception v1

    monitor-exit v0
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    throw v1
.end method

.method private postResult(Ljava/lang/Object;)Ljava/lang/Object;
    .locals 5
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(TResult;)TResult;"
        }
    .end annotation

    invoke-direct {p0}, Lcom/androlua/util/AsyncTaskX;->getHandler()Landroid/os/Handler;

    move-result-object v0

    new-instance v1, Lcom/androlua/util/AsyncTaskX$AsyncTaskResult;

    const/4 v2, 0x1

    new-array v3, v2, [Ljava/lang/Object;

    const/4 v4, 0x0

    aput-object p1, v3, v4

    invoke-direct {v1, p0, v3}, Lcom/androlua/util/AsyncTaskX$AsyncTaskResult;-><init>(Lcom/androlua/util/AsyncTaskX;[Ljava/lang/Object;)V

    invoke-virtual {v0, v2, v1}, Landroid/os/Handler;->obtainMessage(ILjava/lang/Object;)Landroid/os/Message;

    move-result-object v0

    invoke-virtual {v0}, Landroid/os/Message;->sendToTarget()V

    return-object p1
.end method

.method private postResultIfNotInvoked(Ljava/lang/Object;)V
    .locals 1
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(TResult;)V"
        }
    .end annotation

    iget-object v0, p0, Lcom/androlua/util/AsyncTaskX;->mTaskInvoked:Ljava/util/concurrent/atomic/AtomicBoolean;

    invoke-virtual {v0}, Ljava/util/concurrent/atomic/AtomicBoolean;->get()Z

    move-result v0

    if-nez v0, :cond_0

    invoke-direct {p0, p1}, Lcom/androlua/util/AsyncTaskX;->postResult(Ljava/lang/Object;)Ljava/lang/Object;

    :cond_0
    return-void
.end method

.method public static setDefaultExecutor(Ljava/util/concurrent/Executor;)V
    .locals 0

    sput-object p0, Lcom/androlua/util/AsyncTaskX;->sDefaultExecutor:Ljava/util/concurrent/Executor;

    return-void
.end method


# virtual methods
.method public cancel()Z
    .locals 1

    const/4 v0, 0x0

    invoke-virtual {p0, v0}, Lcom/androlua/util/AsyncTaskX;->cancel(Z)Z

    move-result v0

    return v0
.end method

.method public final cancel(Z)Z
    .locals 2

    iget-object v0, p0, Lcom/androlua/util/AsyncTaskX;->mCancelled:Ljava/util/concurrent/atomic/AtomicBoolean;

    const/4 v1, 0x1

    invoke-virtual {v0, v1}, Ljava/util/concurrent/atomic/AtomicBoolean;->set(Z)V

    iget-object v0, p0, Lcom/androlua/util/AsyncTaskX;->mFuture:Ljava/util/concurrent/FutureTask;

    invoke-virtual {v0, p1}, Ljava/util/concurrent/FutureTask;->cancel(Z)Z

    move-result p1

    return p1
.end method

.method protected varargs abstract doInBackground([Ljava/lang/Object;)Ljava/lang/Object;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "([TParams;)TResult;"
        }
    .end annotation
.end method

.method public final varargs execute([Ljava/lang/Object;)Lcom/androlua/util/AsyncTaskX;
    .locals 1
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "([TParams;)",
            "Lcom/androlua/util/AsyncTaskX<",
            "TParams;TProgress;TResult;>;"
        }
    .end annotation

    sget-object v0, Lcom/androlua/util/AsyncTaskX;->sDefaultExecutor:Ljava/util/concurrent/Executor;

    invoke-virtual {p0, v0, p1}, Lcom/androlua/util/AsyncTaskX;->executeOnExecutor(Ljava/util/concurrent/Executor;[Ljava/lang/Object;)Lcom/androlua/util/AsyncTaskX;

    move-result-object p1

    return-object p1
.end method

.method public final varargs executeOnExecutor(Ljava/util/concurrent/Executor;[Ljava/lang/Object;)Lcom/androlua/util/AsyncTaskX;
    .locals 2
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/util/concurrent/Executor;",
            "[TParams;)",
            "Lcom/androlua/util/AsyncTaskX<",
            "TParams;TProgress;TResult;>;"
        }
    .end annotation

    iget-object v0, p0, Lcom/androlua/util/AsyncTaskX;->mStatus:Lcom/androlua/util/AsyncTaskX$Status;

    sget-object v1, Lcom/androlua/util/AsyncTaskX$Status;->PENDING:Lcom/androlua/util/AsyncTaskX$Status;

    if-eq v0, v1, :cond_2

    sget-object v0, Lcom/androlua/util/AsyncTaskX$4;->$SwitchMap$com$androlua$util$AsyncTaskX$Status:[I

    iget-object v1, p0, Lcom/androlua/util/AsyncTaskX;->mStatus:Lcom/androlua/util/AsyncTaskX$Status;

    invoke-virtual {v1}, Ljava/lang/Enum;->ordinal()I

    move-result v1

    aget v0, v0, v1

    const/4 v1, 0x1

    if-eq v0, v1, :cond_1

    const/4 v1, 0x2

    if-eq v0, v1, :cond_0

    goto :goto_0

    :cond_0
    new-instance p1, Ljava/lang/IllegalStateException;

    const-string p2, "Cannot execute task: the task has already been executed (a task can be executed only once)"

    invoke-direct {p1, p2}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw p1

    :cond_1
    new-instance p1, Ljava/lang/IllegalStateException;

    const-string p2, "Cannot execute task: the task is already running."

    invoke-direct {p1, p2}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw p1

    :cond_2
    :goto_0
    sget-object v0, Lcom/androlua/util/AsyncTaskX$Status;->RUNNING:Lcom/androlua/util/AsyncTaskX$Status;

    iput-object v0, p0, Lcom/androlua/util/AsyncTaskX;->mStatus:Lcom/androlua/util/AsyncTaskX$Status;

    invoke-virtual {p0}, Lcom/androlua/util/AsyncTaskX;->onPreExecute()V

    iget-object v0, p0, Lcom/androlua/util/AsyncTaskX;->mWorker:Lcom/androlua/util/AsyncTaskX$WorkerRunnable;

    iput-object p2, v0, Lcom/androlua/util/AsyncTaskX$WorkerRunnable;->mParams:[Ljava/lang/Object;

    iget-object p2, p0, Lcom/androlua/util/AsyncTaskX;->mFuture:Ljava/util/concurrent/FutureTask;

    invoke-interface {p1, p2}, Ljava/util/concurrent/Executor;->execute(Ljava/lang/Runnable;)V

    return-object p0
.end method

.method public final get()Ljava/lang/Object;
    .locals 1
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()TResult;"
        }
    .end annotation

    iget-object v0, p0, Lcom/androlua/util/AsyncTaskX;->mFuture:Ljava/util/concurrent/FutureTask;

    invoke-virtual {v0}, Ljava/util/concurrent/FutureTask;->get()Ljava/lang/Object;

    move-result-object v0

    return-object v0
.end method

.method public final get(JLjava/util/concurrent/TimeUnit;)Ljava/lang/Object;
    .locals 1
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(J",
            "Ljava/util/concurrent/TimeUnit;",
            ")TResult;"
        }
    .end annotation

    iget-object v0, p0, Lcom/androlua/util/AsyncTaskX;->mFuture:Ljava/util/concurrent/FutureTask;

    invoke-virtual {v0, p1, p2, p3}, Ljava/util/concurrent/FutureTask;->get(JLjava/util/concurrent/TimeUnit;)Ljava/lang/Object;

    move-result-object p1

    return-object p1
.end method

.method public final getStatus()Lcom/androlua/util/AsyncTaskX$Status;
    .locals 1

    iget-object v0, p0, Lcom/androlua/util/AsyncTaskX;->mStatus:Lcom/androlua/util/AsyncTaskX$Status;

    return-object v0
.end method

.method public final isCancelled()Z
    .locals 1

    iget-object v0, p0, Lcom/androlua/util/AsyncTaskX;->mCancelled:Ljava/util/concurrent/atomic/AtomicBoolean;

    invoke-virtual {v0}, Ljava/util/concurrent/atomic/AtomicBoolean;->get()Z

    move-result v0

    return v0
.end method

.method protected onCancelled()V
    .locals 0

    return-void
.end method

.method protected onCancelled(Ljava/lang/Object;)V
    .locals 0
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(TResult;)V"
        }
    .end annotation

    invoke-virtual {p0}, Lcom/androlua/util/AsyncTaskX;->onCancelled()V

    return-void
.end method

.method protected onPostExecute(Ljava/lang/Object;)V
    .locals 0
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(TResult;)V"
        }
    .end annotation

    return-void
.end method

.method protected onPreExecute()V
    .locals 0

    return-void
.end method

.method protected varargs onProgressUpdate([Ljava/lang/Object;)V
    .locals 0
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "([TProgress;)V"
        }
    .end annotation

    return-void
.end method

.method protected final varargs publishProgress([Ljava/lang/Object;)V
    .locals 2
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "([TProgress;)V"
        }
    .end annotation

    invoke-virtual {p0}, Lcom/androlua/util/AsyncTaskX;->isCancelled()Z

    move-result v0

    if-nez v0, :cond_0

    invoke-direct {p0}, Lcom/androlua/util/AsyncTaskX;->getHandler()Landroid/os/Handler;

    move-result-object v0

    new-instance v1, Lcom/androlua/util/AsyncTaskX$AsyncTaskResult;

    invoke-direct {v1, p0, p1}, Lcom/androlua/util/AsyncTaskX$AsyncTaskResult;-><init>(Lcom/androlua/util/AsyncTaskX;[Ljava/lang/Object;)V

    const/4 p1, 0x2

    invoke-virtual {v0, p1, v1}, Landroid/os/Handler;->obtainMessage(ILjava/lang/Object;)Landroid/os/Message;

    move-result-object p1

    invoke-virtual {p1}, Landroid/os/Message;->sendToTarget()V

    :cond_0
    return-void
.end method
