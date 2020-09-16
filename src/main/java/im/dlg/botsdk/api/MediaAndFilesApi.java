package im.dlg.botsdk.api;

import im.dlg.grpc.services.MediaAndFilesGrpc;
import im.dlg.grpc.services.MediaAndFilesOuterClass.*;
import im.dlg.botsdk.internal.InternalBot;
import io.grpc.ManagedChannel;
import org.asynchttpclient.AsyncHttpClient;
import org.javatuples.Pair;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class MediaAndFilesApi {

    private final ManagedChannel channel;
    private final InternalBot internalBot;
    private final AsyncHttpClient asyncHttpClient;

    public MediaAndFilesApi(ManagedChannel channel, InternalBot internalBot, AsyncHttpClient asyncHttpClient) {
        this.channel = channel;
        this.internalBot = internalBot;
        this.asyncHttpClient = asyncHttpClient;
    }

    /**
     * Uploads a file to server
     *
     * @param file     - file to upload
     * @return A CompletableFuture for FileLocation on server side (FileLocation for
     * Dialog internal api)
     */
    public CompletableFuture<FileLocation> uploadFile(@Nonnull File file) {
        if (!file.isFile()) {
            throw new IllegalArgumentException("Input isn't a file !");
        }

        String fileName = file.getName();
        int fileSize = (int) file.length();

        RequestGetFileUploadUrl.Builder requestGetUrl = RequestGetFileUploadUrl.newBuilder()
                .setExpectedSize(fileSize);

        return internalBot
                .withToken(MediaAndFilesGrpc.newFutureStub(channel).withDeadlineAfter(1,
                        TimeUnit.MINUTES), stub -> stub.getFileUploadUrl(requestGetUrl.build())
                ).thenCompose(responseGetFileUploadUrl -> {
                    RequestGetFileUploadPartUrl.Builder uploadPart =
                            RequestGetFileUploadPartUrl.newBuilder().setPartNumber(0).
                                    setPartSize(fileSize).setUploadKey(responseGetFileUploadUrl.getUploadKey());
                    return internalBot
                            .withToken(MediaAndFilesGrpc.newFutureStub(channel).withDeadlineAfter(3,
                                    TimeUnit.MINUTES), stub -> stub.getFileUploadPartUrl(uploadPart.build()))
                            .thenApply(res -> new Pair<>(res.getUrl(), responseGetFileUploadUrl.getUploadKey()));
                }).thenCompose(respPair ->
                        asyncHttpClient.preparePut(respPair.getValue0())
                                .setBody(file).execute().toCompletableFuture()
                                .thenApply(val -> respPair.getValue1())
                ).thenCompose(uploadKey -> internalBot
                        .withToken(MediaAndFilesGrpc.newFutureStub(channel).withDeadlineAfter(3,
                                TimeUnit.MINUTES), stub ->
                                stub.commitFileUpload(RequestCommitFileUpload.newBuilder()
                                        .setFileName(fileName)
                                        .setUploadKey(uploadKey).build()))
                        .thenApply(ResponseCommitFileUpload::getUploadedFileLocation));
    }

    /**
     * Get Url to download a file from server
     *
     * @param fileLoc - file location (dialog internal API)
     * @return - A CompletableFuture for a String containing download url
     */
    public CompletableFuture<String> getFileUrl(FileLocation fileLoc) {
        return internalBot.withToken(
                        MediaAndFilesGrpc.newFutureStub(channel).withDeadlineAfter(2, TimeUnit.MINUTES),
                        stub -> stub.getFileUrls(RequestGetFileUrls.newBuilder().addFiles(fileLoc).build()))
                .thenApply(resp -> resp.getFileUrlsList().get(0).getUrl());
    }

}