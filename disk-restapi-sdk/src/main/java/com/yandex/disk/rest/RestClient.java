/*
* (C) 2015 Yandex LLC (https://yandex.com/)
*
* The source code of Java SDK for Yandex.Disk REST API
* is available to use under terms of Apache License,
* Version 2.0. See the file LICENSE for the details.
*/

package com.yandex.disk.rest;

import com.yandex.disk.rest.exceptions.ServerException;
import com.yandex.disk.rest.exceptions.ServerIOException;
import com.yandex.disk.rest.exceptions.WrongMethodException;
import com.yandex.disk.rest.exceptions.http.HttpCodeException;
import com.yandex.disk.rest.json.ApiVersion;
import com.yandex.disk.rest.json.DiskInfo;
import com.yandex.disk.rest.json.Link;
import com.yandex.disk.rest.json.Operation;
import com.yandex.disk.rest.json.Resource;
import com.yandex.disk.rest.json.ResourceList;
import com.yandex.disk.rest.retrofit.CloudApi;
import com.yandex.disk.rest.retrofit.RequestInterceptor;
import com.yandex.disk.rest.util.Hash;
import com.yandex.disk.rest.util.Logger;
import com.yandex.disk.rest.util.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.yandex.disk.rest.retrofit.ErrorHandler.throwHttpCodeException;

public class RestClient {
    
    private static final Logger logger = LoggerFactory.getLogger(RestClient.class);
	
    private static final String CLOUD_API_BASE_URL = "https://cloud-api.yandex.net";

    private final OkHttpClient client;
	
    private final String serverURL;
	
    private final CloudApi cloudApi;

	public RestClient(final Credentials credentials) {
		try {
			this.serverURL = new URL(CLOUD_API_BASE_URL).toExternalForm();
		} catch (MalformedURLException ex) {
			throw new RuntimeException(ex);
		}

		OkHttpClient.Builder okhttpbuilder = OkHttpClientFactory.makeClient();
		okhttpbuilder.addInterceptor(new RequestInterceptor(credentials.getHeaders()));
		this.client = okhttpbuilder.build();

		Retrofit.Builder builder = new Retrofit.Builder()
				.client(client)
				.baseUrl(getUrl())
				.addConverterFactory(GsonConverterFactory.create());

        this.cloudApi = builder.build().create(CloudApi.class);
    }
	
    /* package */ String getUrl() {
        return serverURL;
    }

	/* package */ OkHttpClient getClient() {
        return client;
    }
	
    private <T> T processResponse(Response<T> response) throws HttpCodeException {
        return response.isSuccessful() ? response.body() : throwHttpCodeException(response);
    }

    /**
     * Server API version and build
     */
    public ApiVersion getApiVersion() throws IOException, ServerIOException {
        return processResponse(cloudApi.getApiVersion().execute());
    }

    /**
     * Operation status
     *
     * @see <p>API reference <a href="http://api.yandex.com/disk/api/reference/operations.xml">english</a>,
     * <a href="https://tech.yandex.ru/disk/api/reference/operations-docpage/">russian</a></p>
     */
    public Operation getOperation(final String operationId) throws IOException, ServerIOException {
        return processResponse(cloudApi.getOperation(operationId).execute());
    }

    /**
     * Operation status
     *
     * @see <p>API reference <a href="http://api.yandex.com/disk/api/reference/operations.xml">english</a>,
     * <a href="https://tech.yandex.ru/disk/api/reference/operations-docpage/">russian</a></p>
     */
    public Operation getOperation(final Link link) throws IOException, WrongMethodException, HttpCodeException {
        if (!"GET".equalsIgnoreCase(link.getMethod())) {
            throw new WrongMethodException("Method in Link object is not GET");
        }
        final Operation operation = new RestClientIO(client).getOperation(link.getHref());
        logger.debug("getOperation: " + operation);
        return operation;
    }

    /**
     * Waiting operation to stop
     *
     * @see <p>API reference <a href="http://api.yandex.com/disk/api/reference/operations.xml">english</a>,
     * <a href="https://tech.yandex.ru/disk/api/reference/operations-docpage/">russian</a></p>
     */
    public Operation waitProgress(final Link link, final Runnable waiting)
            throws IOException, WrongMethodException, HttpCodeException {
        while (true) {
            final Operation operation = getOperation(link);
            if (!operation.isInProgress()) {
                return operation;
            }
            waiting.run();
        }
    }

    /**
     * Data about a user's Disk
     *
     * @see <p>API reference <a href="http://api.yandex.com/disk/api/reference/capacity.xml">english</a>,
     * <a href="https://tech.yandex.ru/disk/api/reference/capacity-docpage/">russian</a></p>
     */
    public DiskInfo getDiskInfo() throws IOException, ServerIOException {
        return getDiskInfo(null);
    }

    /**
     * Data about a user's Disk
     *
     * @see <p>API reference <a href="http://api.yandex.com/disk/api/reference/capacity.xml">english</a>,
     * <a href="https://tech.yandex.ru/disk/api/reference/capacity-docpage/">russian</a></p>
     */
    public DiskInfo getDiskInfo(final String fields) throws IOException, ServerIOException {
        return processResponse(cloudApi.getDiskInfo(fields).execute());
    }

    /**
     * Metainformation about a file or folder
     *
     * @see <p>API reference <a href="http://api.yandex.com/disk/api/reference/meta.xml">english</a>,
     * <a href="https://tech.yandex.ru/disk/api/reference/meta-docpage/">russian</a></p>
     */
    public Resource getResources(final ResourcesArgs args) throws IOException, ServerIOException {
        final Resource resource = processResponse(cloudApi.getResources(args.getPath(),
                args.getFields(), args.getLimit(), args.getOffset(), args.getSort(),
                args.getPreviewSize(), args.getPreviewCrop())
                .execute());
        if (args.getParsingHandler() != null) {
            parseListResponse(resource, args.getParsingHandler());
        }
        return resource;
    }

    /**
     * Flat list of all files
     *
     * @see <p>API reference <a href="http://api.yandex.com/disk/api/reference/all-files.xml">english</a>,
     * <a href="https://tech.yandex.ru/disk/api/reference/all-files-docpage/">russian</a></p>
     */
    public ResourceList getFlatResourceList(final ResourcesArgs args) throws IOException, ServerIOException {
        final ResourceList resourceList = processResponse(cloudApi.getFlatResourceList(args.getLimit(),
                args.getMediaType(), args.getOffset(), args.getFields(), args.getPreviewSize(),
                args.getPreviewCrop())
                .execute());
        if (args.getParsingHandler() != null) {
            parseListResponse(resourceList, args.getParsingHandler());
        }
        return resourceList;
    }

    /**
     * Latest uploaded files
     *
     * @see <p>API reference <a href="http://api.yandex.com/disk/api/reference/recent-upload.xml">english</a>,
     * <a href="https://tech.yandex.ru/disk/api/reference/recent-upload-docpage/">russian</a></p>
     */
    public ResourceList getLastUploadedResources(final ResourcesArgs args) throws IOException, ServerIOException {
        final ResourceList resourceList = processResponse(cloudApi.getLastUploadedResources(args.getLimit(),
                args.getMediaType(), args.getOffset(), args.getFields(), args.getPreviewSize(),
                args.getPreviewCrop())
                .execute());
        if (args.getParsingHandler() != null) {
            parseListResponse(resourceList, args.getParsingHandler());
        }
        return resourceList;
    }

    /**
     * Latest uploaded files
     *
     * @see <p>API reference <a href="http://api.yandex.com/disk/api/reference/meta-add.xml">english</a>,
     * <a href="https://tech.yandex.ru/disk/api/reference/meta-add-docpage/">russian</a></p>
     */
    public Resource patchResource(final ResourcesArgs args) throws ServerIOException, IOException {
        final Resource resource = processResponse(cloudApi.patchResource(args.getPath(), args.getFields(),
                args.getBody())
                .execute());
        if (args.getParsingHandler() != null) {
            parseListResponse(resource, args.getParsingHandler());
        }
        return resource;
    }

    /**
     * Metainformation about a public resource
     *
     * @see <p>API reference <a href="http://api.yandex.com/disk/api/reference/public.xml">english</a>,
     * <a href="https://tech.yandex.ru/disk/api/reference/public-docpage/">russian</a></p>
     */
    public Resource listPublicResources(final ResourcesArgs args) throws IOException, ServerIOException {
        final Resource resource = processResponse(cloudApi.listPublicResources(args.getPublicKey(),
                args.getPath(), args.getFields(), args.getLimit(), args.getOffset(), args.getSort(),
                args.getPreviewSize(), args.getPreviewCrop())
                .execute());
        if (args.getParsingHandler() != null) {
            parseListResponse(resource, args.getParsingHandler());
        }
        return resource;
    }

    /**
     * Metainformation about a file or folder in the Trash
     *
     * @see <p>API reference <a href="http://api.yandex.com/disk/api/reference/meta.xml">english</a>,
     * <a href="https://tech.yandex.ru/disk/api/reference/meta-docpage/">russian</a></p>
     */
    public Resource getTrashResources(final ResourcesArgs args) throws IOException, ServerIOException {
        final Resource resource = processResponse(cloudApi.getTrashResources(args.getPath(),
                args.getFields(), args.getLimit(), args.getOffset(), args.getSort(),
                args.getPreviewSize(), args.getPreviewCrop())
                .execute());
        if (args.getParsingHandler() != null) {
            parseListResponse(resource, args.getParsingHandler());
        }
        return resource;
    }

    /**
     * Cleaning the Trash
     *
     * @see <p>API reference <a href="http://api.yandex.com/disk/api/reference/trash-delete.xml">english</a>,
     * <a href="https://tech.yandex.ru/disk/api/reference/trash-delete-docpage/">russian</a></p>
     */
    public Link deleteFromTrash(final String path)
            throws IOException, ServerIOException {
        return new RestClientIO(client)
                .delete(new QueryBuilder(getUrl() + "/v1/disk/trash/resources")
                        .add("path", path)
                        .build());
    }

    /**
     * Restoring a file or folder from the Trash
     *
     * @see <p>API reference <a href="http://api.yandex.com/disk/api/reference/trash-restore.xml">english</a>,
     * <a href="https://tech.yandex.ru/disk/api/reference/trash-restore-docpage/">russian</a></p>
     */
    public Link restoreFromTrash(final String path, final String name, final Boolean overwrite)
            throws IOException, ServerIOException {
        return new RestClientIO(client)
                .put(new QueryBuilder(getUrl() + "/v1/disk/trash/resources/restore")
                        .add("path", path)
                        .add("name", name)
                        .add("overwrite", overwrite)
                        .build());
    }

    private void parseListResponse(final Resource resource, final ResourcesHandler handler) {
        handler.handleSelf(resource);
        final ResourceList items = resource.getResourceList();
        int size = 0;
        if (items != null) {
            size = items.getItems().size();
			//noinspection Convert2streamapi
			for (final Resource item : items.getItems()) {
                handler.handleItem(item);
            }
        }
        handler.onFinished(size);
    }

    private void parseListResponse(final ResourceList resourceList, final ResourcesHandler handler) {
        List<Resource> items = resourceList.getItems();
        int size = 0;
        if (items != null) {
            size = items.size();
			//noinspection Convert2streamapi
			for (Resource item : items) {
                handler.handleItem(item);
            }
        }
        handler.onFinished(size);
    }

    /**
     * Downloading a file from Disk
     *
     * @see <p>API reference <a href="http://api.yandex.com/disk/api/reference/content.xml">english</a>,
     * <a href="https://tech.yandex.ru/disk/api/reference/content-docpage/">russian</a></p>
     */
    public void downloadFile(final String path, final File saveTo, final ProgressListener progressListener)
            throws IOException, ServerException {
        final Link link = processResponse(cloudApi.getDownloadLink(path).execute());
        new RestClientIO(client).downloadUrl(link.getHref(), new FileDownloadListener(saveTo, progressListener));
    }

    /**
     * Downloading a file from Disk
     *
     * @see <p>API reference <a href="http://api.yandex.com/disk/api/reference/content.xml">english</a>,
     * <a href="https://tech.yandex.ru/disk/api/reference/content-docpage/">russian</a></p>
     */
    public void downloadFile(final String path, final DownloadListener downloadListener)
            throws IOException, ServerException {
        final Link link = processResponse(cloudApi.getDownloadLink(path).execute());
        new RestClientIO(client).downloadUrl(link.getHref(), downloadListener);
    }

    /**
     * Uploading a file to Disk from external resource
     *
     * @see <p>API reference <a href="http://api.yandex.com/disk/api/reference/upload-ext.xml">english</a>,
     * <a href="https://tech.yandex.ru/disk/api/reference/upload-ext-docpage/">russian</a></p>
     */
    public Link saveFromUrl(final String url, final String serverPath)
            throws ServerIOException, IOException {
        return processResponse(cloudApi.saveFromUrl(url, serverPath).execute());
    }

    /**
     * Uploading a file to Disk: get Link to upload
     *
     * @see <p>API reference <a href="http://api.yandex.com/disk/api/reference/upload.xml">english</a>,
     * <a href="https://tech.yandex.ru/disk/api/reference/upload-docpage/">russian</a></p>
     */
    public Link getUploadLink(final String serverPath, final boolean overwrite)
            throws ServerIOException, WrongMethodException, IOException {
        final Link link = processResponse(cloudApi.getUploadLink(serverPath, overwrite)
                .execute());
        if (!"PUT".equalsIgnoreCase(link.getMethod())) {
            throw new WrongMethodException("Method in Link object is not PUT");
        }
        return link;
    }

    /**
     * Uploading a file to Disk: upload a file
     *
     * @see <p>API reference <a href="http://api.yandex.com/disk/api/reference/upload.xml">english</a>,
     * <a href="https://tech.yandex.ru/disk/api/reference/upload-docpage/">russian</a></p>
     */
    public void uploadFile(final Link link, final boolean resumeUpload, final File localSource,
                           final ProgressListener progressListener)
            throws IOException, ServerException {
        RestClientIO clientIO = new RestClientIO(client);
        long startOffset = 0;
        if (resumeUpload) {
            Hash hash = Hash.getHash(localSource);
            startOffset = clientIO.getUploadedSize(link.getHref(), hash);
            logger.debug("head: startOffset=" + startOffset);
        }
        clientIO.uploadFile(link.getHref(), localSource, startOffset, progressListener);
    }

    /**
     * Deleting a file or folder
     *
     * @see <p>API reference <a href="http://api.yandex.com/disk/api/reference/delete.xml">english</a>,
     * <a href="https://tech.yandex.ru/disk/api/reference/delete-docpage/">russian</a></p>
     */
    public Link delete(final String path, final boolean permanently)
            throws ServerIOException, IOException {
        return new RestClientIO(client)
                .delete(new QueryBuilder(getUrl() + "/v1/disk/resources")
                        .add("path", path)
                        .add("permanently", permanently)
                        .build());
    }

    /**
     * Creating a folder
     *
     * @see <p>API reference <a href="http://api.yandex.com/disk/api/reference/create-folder.xml">english</a>,
     * <a href="https://tech.yandex.ru/disk/api/reference/create-folder-docpage/">russian</a></p>
     */
    public Link makeFolder(final String path) throws ServerIOException, IOException {
        return processResponse(cloudApi.makeFolder(path).execute());
    }

    /**
     * Copying a file or folder
     *
     * @see <p>API reference <a href="http://api.yandex.com/disk/api/reference/copy.xml">english</a>,
     * <a href="https://tech.yandex.ru/disk/api/reference/copy-docpage/">russian</a></p>
     */
    public Link copy(final String from, final String path, final boolean overwrite)
            throws ServerIOException, IOException {
        return processResponse(cloudApi.copy(from, path, overwrite).execute());
    }

    /**
     * Moving a file or folder
     *
     * @see <p>API reference <a href="http://api.yandex.com/disk/api/reference/move.xml">english</a>,
     * <a href="https://tech.yandex.ru/disk/api/reference/move-docpage/">russian</a></p>
     */
    public Link move(final String from, final String path, final boolean overwrite)
            throws ServerIOException, IOException {
        return processResponse(cloudApi.move(from, path, overwrite).execute());
    }

    /**
     * Publishing a file or folder
     *
     * @see <p>API reference <a href="http://api.yandex.com/disk/api/reference/publish.xml">english</a>,
     * <a href="https://tech.yandex.ru/disk/api/reference/publish-docpage/">russian</a></p>
     */
    public Link publish(final String path)
            throws ServerIOException, IOException {
        return processResponse(cloudApi.publish(path).execute());
    }

    /**
     * Closing access to a resource
     *
     * @see <p>API reference <a href="http://api.yandex.com/disk/api/reference/publish.xml">english</a>,
     * <a href="https://tech.yandex.ru/disk/api/reference/publish-docpage/">russian</a></p>
     */
    public Link unpublish(final String path)
            throws ServerIOException, IOException {
        return processResponse(cloudApi.unpublish(path).execute());
    }

    /**
     * Downloading a public file or folder
     *
     * @see <p>API reference <a href="http://api.yandex.com/disk/api/reference/public.xml#download">english</a>,
     * <a href="https://tech.yandex.ru/disk/api/reference/public-docpage/#download">russian</a></p>
     */
    public void downloadPublicResource(final String publicKey, final String path,
                                       final File saveTo, final ProgressListener progressListener)
            throws IOException, ServerException {
        final Link link = processResponse(cloudApi.getPublicResourceDownloadLink(publicKey, path)
                .execute());
        new RestClientIO(client).downloadUrl(link.getHref(), new FileDownloadListener(saveTo, progressListener));
    }

    /**
     * Saving a public file in "Downloads"
     *
     * @see <p>API reference <a href="http://api.yandex.com/disk/api/reference/public.xml#save">english</a>,
     * <a href="https://tech.yandex.ru/disk/api/reference/public-docpage/#save">russian</a></p>
     */
    public Link savePublicResource(final String publicKey, final String path, final String name)
            throws IOException, ServerException {
        return processResponse(cloudApi.savePublicResource(publicKey, path, name).execute());
    }
}