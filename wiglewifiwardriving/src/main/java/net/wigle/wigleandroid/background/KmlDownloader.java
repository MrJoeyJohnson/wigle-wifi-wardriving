package net.wigle.wigleandroid.background;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import net.wigle.wigleandroid.DatabaseHelper;
import net.wigle.wigleandroid.MainActivity;
import net.wigle.wigleandroid.WiGLEAuthException;
import net.wigle.wigleandroid.background.AbstractApiRequest;
import net.wigle.wigleandroid.background.AbstractProgressApiRequest;
import net.wigle.wigleandroid.background.ApiListener;
import net.wigle.wigleandroid.background.ObservationUploader;
import net.wigle.wigleandroid.background.Status;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by arkasha on 10/27/17.
 */

public class KmlDownloader extends AbstractProgressApiRequest {
    private Status status;

    public KmlDownloader(final FragmentActivity context, final DatabaseHelper dbHelper /*TODO: not needed?*/,
                               final String transid, final ApiListener listener) {
        super(context, dbHelper, "KmlDL", transid+".kml", MainActivity.KML_TRANSID_URL_STEM+transid, false,
                true, true, false, AbstractApiRequest.REQUEST_GET, listener, true);
        }

    @Override
    protected void subRun() throws IOException, InterruptedException, WiGLEAuthException {
        status = Status.UNKNOWN;
        final Bundle bundle = new Bundle();
        sendBundledMessage( Status.DOWNLOADING.ordinal(), bundle );

        String result = null;
        try {
            result = doDownload(this.connectionMethod);
            writeSharefile(result, cacheFilename);
            final JSONObject json = new JSONObject("{success: " + true + ", file:\"" +
                    (MainActivity.hasSD()? MainActivity.getSDPath() : context.getFilesDir().getAbsolutePath()+"/kml/" ) + cacheFilename + "\"}");
            sendBundledMessage( Status.SUCCESS.ordinal(), bundle );
            listener.requestComplete(json, false);
        } catch (final WiGLEAuthException waex) {
            // ALIBI: allow auth exception through
            sendBundledMessage( Status.FAIL.ordinal(), bundle );
            throw waex;
        } catch (final JSONException ex) {
            sendBundledMessage( Status.FAIL.ordinal(), bundle );
            MainActivity.error("ex: " + ex + " result: " + result, ex);
        }
    }

    protected void writeSharefile(final String result, final String filename) throws IOException {

        if (MainActivity.hasSD()) {
            if (cacheFilename != null) {
                cacheResult(result);
            }
        } else {
            //see if KML dir exists
            MainActivity.info("local storage DL...");
            File kmlPath = context.getDir(context.getFilesDir().getAbsolutePath()+"/kml", Context.MODE_PRIVATE);
            if (kmlPath.exists() && kmlPath.isDirectory()) {
                MainActivity.info("... cache directory found");
                File kmlFile = new File(kmlPath, filename+".kml");
                FileOutputStream out = new FileOutputStream(kmlFile);
                ObservationUploader.writeFos(out, result);
            }
        }
    }
}
