package nz.ac.auckland.bocaj;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class CursorAccessibility extends Service {
    public CursorAccessibility() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
