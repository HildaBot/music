/*
 * Copyright 2017 jamietech
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    private MusicManager music = null;

    public MusicPlugin(final Hilda hilda) {
        super(hilda);
    }

    public final MusicManager getMusicManager() {
        return this.music;
    }

    @Override
    public final void onDisable() {
        for (final MusicServer server : this.music.getServers()) {
            server.sendMessage("Sorry, I'm shutting down mid-queue! See you soon.");
        }
    }

    @Override
    public final void onEnable() {
        this.music = new MusicManager(this.getHilda(), this);
        this.getHilda().getCommandManager().registerChannelCommand(new MusicBaseCommand(this.getHilda(), this.music));
        MusicPlugin.setupLogging();

        final long rotate = Util.getNextMidnightInMillis("GMT+10") - System.currentTimeMillis();
        this.getHilda().getExecutor().scheduleAtFixedRate(new LogRotateTask(this), rotate, 86400000L, TimeUnit.MILLISECONDS); // At midnight then every 24 hours
        Hilda.getLogger().info("Rotating log files in " + Util.getFriendlyTime(rotate));
    }

    public static final void setupLogging() {
        Hilda.getLogger().setUseParentHandlers(false);

        for (final Handler handler : Hilda.getLogger().getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                Hilda.getLogger().removeHandler(handler);
            }
        }

        final ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new LogFormat());
        Hilda.getLogger().addHandler(handler);

        try {
            final File file = new File("log");

            if (!file.isDirectory() && !file.mkdir()) {
                Hilda.getLogger().warning("Attempted to make a folder but failed. Something might go wrong.");
            }

            final FileHandler lfh = new FileHandler("log/hilda_music_" + new SimpleDateFormat("dd-MM-yyyy").format(Calendar.getInstance().getTime()) + ".log", true);
            lfh.setFormatter(new LogFormat());
            Hilda.getLogger().addHandler(lfh);
        } catch (final Exception e) {
            Hilda.getLogger().log(Level.WARNING, "Encountered an exception while initialising music logger", e);
        }

        if (Start.DEBUG) {
            handler.setLevel(Level.FINE);
            Hilda.getLogger().setLevel(Level.FINE);
        }
    }

}
