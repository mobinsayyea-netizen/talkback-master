.class final Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;
.super Ljava/lang/Object;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lcom/androlua/util/TimerX$TimerImpl;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x1a
    name = "TimerHeap"
.end annotation


# instance fields
.field private DEFAULT_HEAP_SIZE:I

.field private deletedCancelledNumber:I

.field private size:I

.field private timers:[Lcom/androlua/util/TimerTaskX;


# direct methods
.method private constructor <init>()V
    .locals 1

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    const/16 v0, 0x100

    iput v0, p0, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->DEFAULT_HEAP_SIZE:I

    new-array v0, v0, [Lcom/androlua/util/TimerTaskX;

    iput-object v0, p0, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->timers:[Lcom/androlua/util/TimerTaskX;

    const/4 v0, 0x0

    iput v0, p0, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->size:I

    iput v0, p0, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->deletedCancelledNumber:I

    return-void
.end method

.method synthetic constructor <init>(Lcom/androlua/util/c;)V
    .locals 0

    invoke-direct {p0}, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;-><init>()V

    return-void
.end method

.method static bridge synthetic a(Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;)I
    .locals 0

    iget p0, p0, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->deletedCancelledNumber:I

    return p0
.end method

.method static bridge synthetic b(Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;I)V
    .locals 0

    iput p1, p0, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->deletedCancelledNumber:I

    return-void
.end method

.method static bridge synthetic c(Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;Lcom/androlua/util/TimerTaskX;)I
    .locals 0

    invoke-direct {p0, p1}, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->getTask(Lcom/androlua/util/TimerTaskX;)I

    move-result p0

    return p0
.end method

.method private downHeap(I)V
    .locals 9

    mul-int/lit8 v0, p1, 0x2

    add-int/lit8 v0, v0, 0x1

    :goto_0
    iget v1, p0, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->size:I

    if-ge v0, v1, :cond_2

    if-lez v1, :cond_2

    add-int/lit8 v2, v0, 0x1

    if-ge v2, v1, :cond_0

    iget-object v1, p0, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->timers:[Lcom/androlua/util/TimerTaskX;

    aget-object v3, v1, v2

    iget-wide v3, v3, Lcom/androlua/util/TimerTaskX;->when:J

    aget-object v1, v1, v0

    iget-wide v5, v1, Lcom/androlua/util/TimerTaskX;->when:J

    cmp-long v1, v3, v5

    if-gez v1, :cond_0

    move v0, v2

    :cond_0
    iget-object v1, p0, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->timers:[Lcom/androlua/util/TimerTaskX;

    aget-object v2, v1, p1

    iget-wide v3, v2, Lcom/androlua/util/TimerTaskX;->when:J

    aget-object v5, v1, v0

    iget-wide v6, v5, Lcom/androlua/util/TimerTaskX;->when:J

    cmp-long v3, v3, v6

    if-gez v3, :cond_1

    goto :goto_1

    :cond_1
    aput-object v5, v1, p1

    aput-object v2, v1, v0

    mul-int/lit8 p1, v0, 0x2

    add-int/lit8 p1, p1, 0x1

    move v8, v0

    move v0, p1

    move p1, v8

    goto :goto_0

    :cond_2
    :goto_1
    return-void
.end method

.method private getTask(Lcom/androlua/util/TimerTaskX;)I
    .locals 3

    const/4 v0, 0x0

    :goto_0
    iget-object v1, p0, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->timers:[Lcom/androlua/util/TimerTaskX;

    array-length v2, v1

    if-ge v0, v2, :cond_1

    aget-object v1, v1, v0

    if-ne v1, p1, :cond_0

    return v0

    :cond_0
    add-int/lit8 v0, v0, 0x1

    goto :goto_0

    :cond_1
    const/4 p1, -0x1

    return p1
.end method

.method private upHeap()V
    .locals 10

    iget v0, p0, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->size:I

    add-int/lit8 v0, v0, -0x1

    add-int/lit8 v1, v0, -0x1

    div-int/lit8 v1, v1, 0x2

    :goto_0
    iget-object v2, p0, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->timers:[Lcom/androlua/util/TimerTaskX;

    aget-object v3, v2, v0

    iget-wide v4, v3, Lcom/androlua/util/TimerTaskX;->when:J

    aget-object v6, v2, v1

    iget-wide v7, v6, Lcom/androlua/util/TimerTaskX;->when:J

    cmp-long v4, v4, v7

    if-gez v4, :cond_0

    aput-object v6, v2, v0

    aput-object v3, v2, v1

    add-int/lit8 v0, v1, -0x1

    div-int/lit8 v0, v0, 0x2

    move v9, v1

    move v1, v0

    move v0, v9

    goto :goto_0

    :cond_0
    return-void
.end method


# virtual methods
.method public adjustMinimum()V
    .locals 1

    const/4 v0, 0x0

    invoke-direct {p0, v0}, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->downHeap(I)V

    return-void
.end method

.method public delete(I)V
    .locals 3

    if-ltz p1, :cond_0

    iget v0, p0, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->size:I

    if-ge p1, v0, :cond_0

    iget-object v1, p0, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->timers:[Lcom/androlua/util/TimerTaskX;

    add-int/lit8 v0, v0, -0x1

    iput v0, p0, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->size:I

    aget-object v2, v1, v0

    aput-object v2, v1, p1

    const/4 v2, 0x0

    aput-object v2, v1, v0

    invoke-direct {p0, p1}, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->downHeap(I)V

    :cond_0
    return-void
.end method

.method public deleteIfCancelled()V
    .locals 2

    const/4 v0, 0x0

    :goto_0
    iget v1, p0, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->size:I

    if-ge v0, v1, :cond_1

    iget-object v1, p0, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->timers:[Lcom/androlua/util/TimerTaskX;

    aget-object v1, v1, v0

    iget-boolean v1, v1, Lcom/androlua/util/TimerTaskX;->cancelled:Z

    if-eqz v1, :cond_0

    iget v1, p0, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->deletedCancelledNumber:I

    add-int/lit8 v1, v1, 0x1

    iput v1, p0, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->deletedCancelledNumber:I

    invoke-virtual {p0, v0}, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->delete(I)V

    add-int/lit8 v0, v0, -0x1

    :cond_0
    add-int/lit8 v0, v0, 0x1

    goto :goto_0

    :cond_1
    return-void
.end method

.method public insert(Lcom/androlua/util/TimerTaskX;)V
    .locals 4

    iget-object v0, p0, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->timers:[Lcom/androlua/util/TimerTaskX;

    array-length v1, v0

    iget v2, p0, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->size:I

    if-ne v1, v2, :cond_0

    mul-int/lit8 v1, v2, 0x2

    new-array v1, v1, [Lcom/androlua/util/TimerTaskX;

    const/4 v3, 0x0

    invoke-static {v0, v3, v1, v3, v2}, Ljava/lang/System;->arraycopy(Ljava/lang/Object;ILjava/lang/Object;II)V

    iput-object v1, p0, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->timers:[Lcom/androlua/util/TimerTaskX;

    :cond_0
    iget-object v0, p0, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->timers:[Lcom/androlua/util/TimerTaskX;

    iget v1, p0, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->size:I

    add-int/lit8 v2, v1, 0x1

    iput v2, p0, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->size:I

    aput-object p1, v0, v1

    invoke-direct {p0}, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->upHeap()V

    return-void
.end method

.method public isEmpty()Z
    .locals 1

    iget v0, p0, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->size:I

    if-nez v0, :cond_0

    const/4 v0, 0x1

    goto :goto_0

    :cond_0
    const/4 v0, 0x0

    :goto_0
    return v0
.end method

.method public minimum()Lcom/androlua/util/TimerTaskX;
    .locals 2

    iget-object v0, p0, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->timers:[Lcom/androlua/util/TimerTaskX;

    const/4 v1, 0x0

    aget-object v0, v0, v1

    return-object v0
.end method

.method public reset()V
    .locals 1

    iget v0, p0, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->DEFAULT_HEAP_SIZE:I

    new-array v0, v0, [Lcom/androlua/util/TimerTaskX;

    iput-object v0, p0, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->timers:[Lcom/androlua/util/TimerTaskX;

    const/4 v0, 0x0

    iput v0, p0, Lcom/androlua/util/TimerX$TimerImpl$TimerHeap;->size:I

    return-void
.end method
