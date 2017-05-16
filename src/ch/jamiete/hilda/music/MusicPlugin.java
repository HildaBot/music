package ch.jamiete.hilda.music;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.LogFormat;
import ch.jamiete.hilda.Start;
import ch.jamiete.hilda.Util;
import ch.jamiete.hilda.music.commands.MusicBaseCommand;
import ch.jamiete.hilda.music.tasks.LogRotateTask;
import ch.jamiete.hilda.plugins.HildaPlugin;

public class MusicPlugin extends HildaPlugin {

    private MusicManager music;

    public MusicPlugin(Hilda hilda) {
        super(hilda);
    }

    @Override
    public void onEnable() {
        this.music = new MusicManager(this.getHilda(), this);
        this.getHilda().getCommandManager().registerChannelCommand(new MusicBaseCommand(this.getHilda(), this.music));
        this.setupLogging();

        final long rotate = this.getHilda().getNextMidnightInMillis("GMT+10") - System.currentTimeMillis();
        this.getHilda().getExecutor().scheduleAtFixedRate(new LogRotateTask(this), rotate, 86400000, TimeUnit.MILLISECONDS); // At midnight then every 24 hours
        Hilda.getLogger().info("Rotating log files in " + Util.getFriendlyTime(rotate));
    }

    public void setupLogging() {
        MusicManager.getLogger().setUseParentHandlers(false);

        for (Handler handler : MusicManager.getLogger().getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                MusicManager.getLogger().removeHandler(handler);
            }
        }

        final ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new LogFormat());
        MusicManager.getLogger().addHandler(handler);

        try {
            final File file = new File("log");

            if (!file.isDirectory()) {
                file.mkdir();
            }

            final FileHandler lfh = new FileHandler("log/hilda_music_" + new SimpleDateFormat("dd-MM-yyyy").format(Calendar.getInstance().getTime()) + ".log", true);
            lfh.setFormatter(new LogFormat());
            MusicManager.getLogger().addHandler(lfh);
        } catch (final Exception e) {
            Hilda.getLogger().log(Level.WARNING, "Encountered an exception while initialising music logger", e);
        }

        if (Start.DEBUG) {
            handler.setLevel(Level.FINE);
            MusicManager.getLogger().setLevel(Level.FINE);
        }
    }

    public MusicManager getMusicManager() {
        return music;
    }

}
