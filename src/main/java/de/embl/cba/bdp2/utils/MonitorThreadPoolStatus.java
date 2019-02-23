package de.embl.cba.bdp2.utils;

import de.embl.cba.bdp2.ui.ProgressBar;
import de.embl.cba.bdp2.logging.IJLazySwingLogger;
import de.embl.cba.bdp2.logging.Logger;
import de.embl.cba.bdp2.saving.SaveCentral;

import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by tischi on 11/04/17.
 */
public class MonitorThreadPoolStatus {

    private static Logger logger = new IJLazySwingLogger();

    public static void showProgressAndWaitUntilDone(List<Future> futures,
                                                    String message,
                                                    int updateFrequencyMilliseconds) {

        long start = System.currentTimeMillis();

        int i = 0;
        while( i != futures.size() )
        {
            i = 0;
            for ( Future f : futures )
            {
                if (f.isDone() ) i++;
            }
            if (!SaveCentral.interruptSavingThreads) {
                logger.progress(message, null, start, i, futures.size());
                ProgressBar.progress = i * 100 / futures.size(); //Updating UI progress bar.
            }
            try {
                Thread.sleep(updateFrequencyMilliseconds);
            } catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

        }

    }

}