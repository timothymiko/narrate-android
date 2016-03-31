package com.datonicgroup.narrate.app.dataprovider.api.googledrive;

import com.datonicgroup.narrate.app.dataprovider.api.googledrive.models.DriveFile;
import com.datonicgroup.narrate.app.dataprovider.api.googledrive.models.DriveFileChangeList;
import com.datonicgroup.narrate.app.dataprovider.api.googledrive.models.DriveFileList;
import com.datonicgroup.narrate.app.dataprovider.api.googledrive.models.DriveFileMetadata;
import com.datonicgroup.narrate.app.dataprovider.api.googledrive.models.DriveFileMetadataRequest;
import com.datonicgroup.narrate.app.dataprovider.api.googledrive.models.DriveStartPageToken;
import com.datonicgroup.narrate.app.models.DriveEntry;

import java.io.File;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.HEAD;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by timothymiko on 1/12/16.
 */
public interface GoogleDriveFileService {

    String BASE_URL = "https://www.googleapis.com";
    String FOLDER_MIME = "application/vnd.google-apps.folder";

    /**
     * Files
     */
    @GET("/drive/v2/files?spaces=appDataFolder")
    Call<DriveFileList> list(@Query("pageToken") String pageToken, @Query("q") String query);

    @POST("/drive/v3/files")
    Call<DriveFileMetadata> createMetadata(@Body DriveFileMetadataRequest request);

    @Multipart
    @POST("/upload/drive/v3/files?uploadType=multipart")
    Call<DriveFileMetadata> create(@Part("metadata") DriveFileMetadataRequest metadata,
                                   @Part("entry") RequestBody file);

    @GET("/drive/v2/files/{fileId}")
    Call<DriveFileMetadata> retrieveMetadata(@Path("fileId") String fileId);

    @GET("/drive/v3/files/{fileId}?alt=media")
    Call<DriveFile> retrieve(@Path("fileId") String fileId);

    @GET("/drive/v3/files/{fileId}?alt=media")
    Call<ResponseBody> retrieveFile(@Path("fileId") String fileId);

    @PATCH("/drive/v3/files/{fileId}")
    Call<DriveFileMetadata> updateMetadata(@Path("fileId") String fileId, @Body DriveFileMetadataRequest request);

    @PATCH("/upload/drive/v3/files/{fileId}")
    Call<DriveFileMetadata> update(@Path("fileId") String fileId, @Body RequestBody body);

    @DELETE("/drive/v3/files/{fileId}")
    Call<Void> delete(@Path("fileId") String fileId);

    /**
     * Changes
     */
    @GET("/drive/v3/changes/startPageToken")
    Call<DriveStartPageToken> retrieveCurrentChangesToken();

    @GET("/drive/v2/changes?spaces=appDataFolder")
    Call<DriveFileChangeList> listChanges(@Query("pageToken") String token);

}
