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
package ch.jamiete.hilda.music.commands;

import java.util.Arrays;
import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.commands.ChannelSeniorCommand;
import ch.jamiete.hilda.music.MusicManager;

public class MusicBaseCommand extends ChannelSeniorCommand {

    public MusicBaseCommand(final Hilda hilda, final MusicManager manager) {
        super(hilda);

        this.setName("music");
        this.setAliases(Arrays.asList(new String[] { "m" }));
        this.setDescription("Manages the music player.");

        this.registerSubcommand(new MusicAdminCommand(hilda, this, manager));
        this.registerSubcommand(new MusicForceskipCommand(hilda, this, manager));
        this.registerSubcommand(new MusicNowPlayingCommand(hilda, this, manager));
        this.registerSubcommand(new MusicPlayCommand(hilda, this, manager));
        this.registerSubcommand(new MusicQueueCommand(hilda, this, manager));
        this.registerSubcommand(new MusicRemoveCommand(hilda, this, manager));
        this.registerSubcommand(new MusicResetCommand(hilda, this, manager));
        this.registerSubcommand(new MusicShuffleCommand(hilda, this, manager));
        this.registerSubcommand(new MusicSkipCommand(hilda, this, manager));
        this.registerSubcommand(new MusicVolumeCommand(hilda, this, manager));
    }

}
