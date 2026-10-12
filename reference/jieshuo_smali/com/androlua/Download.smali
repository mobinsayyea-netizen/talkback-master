.class public Lcom/androlua/Download;
.super Ljava/lang/Object;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lcom/androlua/Download$OnDownloadCompleteListener;,
        Lcom/androlua/Download$DownloadBroadcastReceiver;
    }
.end annotation


# instance fields
.field private file_input_field:Landroid/widget/EditText;

.field private mContentLength:J

.field private final mContext:Landroid/content/Context;

.field private mDestinationDir:Ljava/lang/String;

.field private mDownload:Landroid/util/LongSparseArray;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Landroid/util/LongSparseArray<",
            "[",
            "Ljava/lang/String;",
            ">;"
        }
    .end annotation
.end field

.field private mDownloadBroadcastReceiver:Lcom/androlua/Download$DownloadBroadcastReceiver;

.field private mFilename:Ljava/lang/String;

.field private mMessage:Ljava/lang/String;

.field private mMimeType:Ljava/lang/String;

.field private mOnDownloadCompleteListener:Lcom/androlua/Download$OnDownloadCompleteListener;

.field private mUrl:Ljava/lang/String;

.field private mUserAgent:Ljava/lang/String;


# direct methods
.method public constructor <init>(Landroid/content/Context;)V
    .locals 1

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    new-instance v0, Landroid/util/LongSparseArray;

    invoke-direct {v0}, Landroid/util/LongSparseArray;-><init>()V

    iput-object v0, p0, Lcom/androlua/Download;->mDownload:Landroid/util/LongSparseArray;

    iput-object p1, p0, Lcom/androlua/Download;->mContext:Landroid/content/Context;

    return-void
.end method

.method static bridge synthetic a(Lcom/androlua/Download;)Landroid/widget/EditText;
    .locals 0

    iget-object p0, p0, Lcom/androlua/Download;->file_input_field:Landroid/widget/EditText;

    return-object p0
.end method

.method static bridge synthetic b(Lcom/androlua/Download;)Landroid/content/Context;
    .locals 0

    iget-object p0, p0, Lcom/androlua/Download;->mContext:Landroid/content/Context;

    return-object p0
.end method

.method static bridge synthetic c(Lcom/androlua/Download;)Landroid/util/LongSparseArray;
    .locals 0

    iget-object p0, p0, Lcom/androlua/Download;->mDownload:Landroid/util/LongSparseArray;

    return-object p0
.end method

.method static bridge synthetic d(Lcom/androlua/Download;)Lcom/androlua/Download$OnDownloadCompleteListener;
    .locals 0

    iget-object p0, p0, Lcom/androlua/Download;->mOnDownloadCompleteListener:Lcom/androlua/Download$OnDownloadCompleteListener;

    return-object p0
.end method

.method static bridge synthetic e(Lcom/androlua/Download;Ljava/lang/String;)V
    .locals 0

    iput-object p1, p0, Lcom/androlua/Download;->mFilename:Ljava/lang/String;

    return-void
.end method


# virtual methods
.method public getContentLength()J
    .locals 2

    iget-wide v0, p0, Lcom/androlua/Download;->mContentLength:J

    return-wide v0
.end method

.method public getDestinationDir()Ljava/lang/String;
    .locals 1

    iget-object v0, p0, Lcom/androlua/Download;->mDestinationDir:Ljava/lang/String;

    return-object v0
.end method

.method public getFilePath()Ljava/lang/String;
    .locals 1

    iget-object v0, p0, Lcom/androlua/Download;->mFilename:Ljava/lang/String;

    return-object v0
.end method

.method public getMimeType()Ljava/lang/String;
    .locals 1

    iget-object v0, p0, Lcom/androlua/Download;->mMimeType:Ljava/lang/String;

    return-object v0
.end method

.method public getUrl()Ljava/lang/String;
    .locals 1

    iget-object v0, p0, Lcom/androlua/Download;->mUrl:Ljava/lang/String;

    return-object v0
.end method

.method public getUserAgent()Ljava/lang/String;
    .locals 1

    iget-object v0, p0, Lcom/androlua/Download;->mUserAgent:Ljava/lang/String;

    return-object v0
.end method

.method public setContentLength(J)V
    .locals 0

    iput-wide p1, p0, Lcom/androlua/Download;->mContentLength:J

    return-void
.end method

.method public setDestinationDir(Ljava/lang/String;)V
    .locals 0

    iput-object p1, p0, Lcom/androlua/Download;->mDestinationDir:Ljava/lang/String;

    return-void
.end method

.method public setFilePath(Ljava/lang/String;)V
    .locals 0

    iput-object p1, p0, Lcom/androlua/Download;->mFilename:Ljava/lang/String;

    return-void
.end method

.method public setMessage(Ljava/lang/String;)V
    .locals 0

    iput-object p1, p0, Lcom/androlua/Download;->mMessage:Ljava/lang/String;

    return-void
.end method

.method public setMimeType(Ljava/lang/String;)V
    .locals 0

    iput-object p1, p0, Lcom/androlua/Download;->mMimeType:Ljava/lang/String;

    return-void
.end method

.method public setOnDownloadCompleteListener(Lcom/androlua/Download$OnDownloadCompleteListener;)V
    .locals 0

    iput-object p1, p0, Lcom/androlua/Download;->mOnDownloadCompleteListener:Lcom/androlua/Download$OnDownloadCompleteListener;

    return-void
.end method

.method public setUrl(Ljava/lang/String;)V
    .locals 0

    iput-object p1, p0, Lcom/androlua/Download;->mUrl:Ljava/lang/String;

    return-void
.end method

.method public setUserAgent(Ljava/lang/String;)V
    .locals 0

    iput-object p1, p0, Lcom/androlua/Download;->mUserAgent:Ljava/lang/String;

    return-void
.end method

.method public start(Z)J
    .locals 5

    iget-object v0, p0, Lcom/androlua/Download;->mDownloadBroadcastReceiver:Lcom/androlua/Download$DownloadBroadcastReceiver;

    if-nez v0, :cond_0

    new-instance v0, Landroid/content/IntentFilter;

    invoke-direct {v0}, Landroid/content/IntentFilter;-><init>()V

    const-string v1, "android.intent.action.DOWNLOAD_COMPLETE"

    invoke-virtual {v0, v1}, Landroid/content/IntentFilter;->addAction(Ljava/lang/String;)V

    new-instance v1, Lcom/androlua/Download$DownloadBroadcastReceiver;

    const/4 v2, 0x0

    invoke-direct {v1, p0, v2}, Lcom/androlua/Download$DownloadBroadcastReceiver;-><init>(Lcom/androlua/Download;Lcom/androlua/a;)V

    iput-object v1, p0, Lcom/androlua/Download;->mDownloadBroadcastReceiver:Lcom/androlua/Download$DownloadBroadcastReceiver;

    iget-object v2, p0, Lcom/androlua/Download;->mContext:Landroid/content/Context;

    invoke-virtual {v2, v1, v0}, Landroid/content/Context;->registerReceiver(Landroid/content/BroadcastReceiver;Landroid/content/IntentFilter;)Landroid/content/Intent;

    :cond_0
    iget-object v0, p0, Lcom/androlua/Download;->mContext:Landroid/content/Context;

    const-string v1, "download"

    invoke-virtual {v0, v1}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Landroid/app/DownloadManager;

    iget-object v1, p0, Lcom/androlua/Download;->mUrl:Ljava/lang/String;

    invoke-static {v1}, Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;

    move-result-object v1

    invoke-virtual {v1}, Landroid/net/Uri;->getLastPathSegment()Ljava/lang/String;

    new-instance v2, Landroid/app/DownloadManager$Request;

    invoke-direct {v2, v1}, Landroid/app/DownloadManager$Request;-><init>(Landroid/net/Uri;)V

    iget-object v1, p0, Lcom/androlua/Download;->mDestinationDir:Ljava/lang/String;

    if-nez v1, :cond_1

    sget-object v1, Landroid/os/Environment;->DIRECTORY_DOWNLOADS:Ljava/lang/String;

    iput-object v1, p0, Lcom/androlua/Download;->mDestinationDir:Ljava/lang/String;

    :cond_1
    iget-object v1, p0, Lcom/androlua/Download;->mDestinationDir:Ljava/lang/String;

    iget-object v3, p0, Lcom/androlua/Download;->mFilename:Ljava/lang/String;

    invoke-virtual {v2, v1, v3}, Landroid/app/DownloadManager$Request;->setDestinationInExternalPublicDir(Ljava/lang/String;Ljava/lang/String;)Landroid/app/DownloadManager$Request;

    iget-object v1, p0, Lcom/androlua/Download;->mFilename:Ljava/lang/String;

    invoke-virtual {v2, v1}, Landroid/app/DownloadManager$Request;->setTitle(Ljava/lang/CharSequence;)Landroid/app/DownloadManager$Request;

    iget-object v1, p0, Lcom/androlua/Download;->mUrl:Ljava/lang/String;

    invoke-virtual {v2, v1}, Landroid/app/DownloadManager$Request;->setDescription(Ljava/lang/CharSequence;)Landroid/app/DownloadManager$Request;

    const/4 v1, 0x2

    if-eqz p1, :cond_2

    invoke-virtual {v2, v1}, Landroid/app/DownloadManager$Request;->setAllowedNetworkTypes(I)Landroid/app/DownloadManager$Request;

    :cond_2
    iget-object p1, p0, Lcom/androlua/Download;->mMimeType:Ljava/lang/String;

    if-nez p1, :cond_3

    const-string p1, "*/*"

    iput-object p1, p0, Lcom/androlua/Download;->mMimeType:Ljava/lang/String;

    :cond_3
    iget-object p1, p0, Lcom/androlua/Download;->mMimeType:Ljava/lang/String;

    invoke-virtual {v2, p1}, Landroid/app/DownloadManager$Request;->setMimeType(Ljava/lang/String;)Landroid/app/DownloadManager$Request;

    invoke-virtual {v0, v2}, Landroid/app/DownloadManager;->enqueue(Landroid/app/DownloadManager$Request;)J

    move-result-wide v2

    new-instance p1, Ljava/io/File;

    iget-object v0, p0, Lcom/androlua/Download;->mDestinationDir:Ljava/lang/String;

    invoke-static {v0}, Landroid/os/Environment;->getExternalStoragePublicDirectory(Ljava/lang/String;)Ljava/io/File;

    move-result-object v0

    iget-object v4, p0, Lcom/androlua/Download;->mFilename:Ljava/lang/String;

    invoke-direct {p1, v0, v4}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V

    invoke-virtual {p1}, Ljava/io/File;->exists()Z

    move-result v0

    if-eqz v0, :cond_4

    invoke-virtual {p1}, Ljava/io/File;->delete()Z

    :cond_4
    iget-object v0, p0, Lcom/androlua/Download;->mDownload:Landroid/util/LongSparseArray;

    new-array v1, v1, [Ljava/lang/String;

    const/4 v4, 0x0

    invoke-virtual {p1}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object p1

    aput-object p1, v1, v4

    const/4 p1, 0x1

    iget-object v4, p0, Lcom/androlua/Download;->mMimeType:Ljava/lang/String;

    aput-object v4, v1, p1

    invoke-virtual {v0, v2, v3, v1}, Landroid/util/LongSparseArray;->put(JLjava/lang/Object;)V

    return-wide v2
.end method

.method public start()V
    .locals 0

    return-void
.end method

.method public start(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
    .locals 0

    iput-object p1, p0, Lcom/androlua/Download;->mUrl:Ljava/lang/String;

    iput-object p4, p0, Lcom/androlua/Download;->mMessage:Ljava/lang/String;

    invoke-static {p1}, Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;

    move-result-object p4

    iput-object p3, p0, Lcom/androlua/Download;->mFilename:Ljava/lang/String;

    if-nez p3, :cond_0

    invoke-virtual {p4}, Landroid/net/Uri;->getLastPathSegment()Ljava/lang/String;

    move-result-object p3

    iput-object p3, p0, Lcom/androlua/Download;->mFilename:Ljava/lang/String;

    :cond_0
    if-nez p2, :cond_1

    sget-object p2, Landroid/os/Environment;->DIRECTORY_DOWNLOADS:Ljava/lang/String;

    iput-object p2, p0, Lcom/androlua/Download;->mDestinationDir:Ljava/lang/String;

    :cond_1
    new-instance p2, Landroid/widget/EditText;

    iget-object p3, p0, Lcom/androlua/Download;->mContext:Landroid/content/Context;

    invoke-direct {p2, p3}, Landroid/widget/EditText;-><init>(Landroid/content/Context;)V

    iput-object p2, p0, Lcom/androlua/Download;->file_input_field:Landroid/widget/EditText;

    iget-object p3, p0, Lcom/androlua/Download;->mFilename:Ljava/lang/String;

    invoke-virtual {p2, p3}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    iget-object p2, p0, Lcom/androlua/Download;->mMessage:Ljava/lang/String;

    if-nez p2, :cond_2

    iput-object p1, p0, Lcom/androlua/Download;->mMessage:Ljava/lang/String;

    :cond_2
    new-instance p1, Landroid/app/AlertDialog$Builder;

    iget-object p2, p0, Lcom/androlua/Download;->mContext:Landroid/content/Context;

    invoke-direct {p1, p2}, Landroid/app/AlertDialog$Builder;-><init>(Landroid/content/Context;)V

    const p2, 0x7f0701f7

    invoke-virtual {p1, p2}, Landroid/app/AlertDialog$Builder;->setTitle(I)Landroid/app/AlertDialog$Builder;

    move-result-object p1

    iget-object p2, p0, Lcom/androlua/Download;->mMessage:Ljava/lang/String;

    invoke-virtual {p1, p2}, Landroid/app/AlertDialog$Builder;->setMessage(Ljava/lang/CharSequence;)Landroid/app/AlertDialog$Builder;

    move-result-object p1

    iget-object p2, p0, Lcom/androlua/Download;->file_input_field:Landroid/widget/EditText;

    invoke-virtual {p1, p2}, Landroid/app/AlertDialog$Builder;->setView(Landroid/view/View;)Landroid/app/AlertDialog$Builder;

    move-result-object p1

    new-instance p2, Lcom/androlua/Download$2;

    invoke-direct {p2, p0}, Lcom/androlua/Download$2;-><init>(Lcom/androlua/Download;)V

    const p3, 0x7f070990

    invoke-virtual {p1, p3, p2}, Landroid/app/AlertDialog$Builder;->setPositiveButton(ILandroid/content/DialogInterface$OnClickListener;)Landroid/app/AlertDialog$Builder;

    move-result-object p1

    iget-object p2, p0, Lcom/androlua/Download;->mContext:Landroid/content/Context;

    const p3, 0x7f070096

    invoke-virtual {p2, p3}, Landroid/content/Context;->getString(I)Ljava/lang/String;

    move-result-object p2

    const/4 p3, 0x0

    invoke-virtual {p1, p2, p3}, Landroid/app/AlertDialog$Builder;->setNegativeButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface$OnClickListener;)Landroid/app/AlertDialog$Builder;

    move-result-object p1

    new-instance p2, Lcom/androlua/Download$1;

    invoke-direct {p2, p0}, Lcom/androlua/Download$1;-><init>(Lcom/androlua/Download;)V

    const p3, 0x7f070992

    invoke-virtual {p1, p3, p2}, Landroid/app/AlertDialog$Builder;->setNeutralButton(ILandroid/content/DialogInterface$OnClickListener;)Landroid/app/AlertDialog$Builder;

    move-result-object p1

    invoke-virtual {p1}, Landroid/app/AlertDialog$Builder;->create()Landroid/app/AlertDialog;

    move-result-object p1

    invoke-virtual {p1}, Landroid/app/Dialog;->show()V

    return-void
.end method
