package ch.jamiete.hilda.music;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.LogFormat;
import ch.jamiete.hilda.Start;
import ch.jamiete.hilda.music.commands.MusicBaseCommand;
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

        MusicManager.getLogger().setUseParentHandlers(false);
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
            e.printStackTrace();
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
