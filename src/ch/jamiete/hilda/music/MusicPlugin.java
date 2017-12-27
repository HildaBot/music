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

import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.music.commands.MusicBaseCommand;
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
    }

}
