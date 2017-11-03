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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import org.apache.commons.lang3.exception.ExceptionUtils;

class MusicLogFormat extends Formatter {
    private final SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd kk:mm:ss");

    @Override
    public final String format(final LogRecord record) {
        String log = "";

        log += this.sdf.format(new Date(record.getMillis()));
        log += " [" + record.getLevel().getLocalizedName().toUpperCase() + ']';

        if (record.getSourceClassName() != null) {
            final String[] split = record.getSourceClassName().split("\\.");
            log += " [" + split[(split.length == 1) ? 0 : (split.length - 1)] + ']';
        }

        if (record.getSourceMethodName() != null) {
            log += " (" + record.getSourceMethodName() + ')';
        }

        log += ' ' + record.getMessage();

        if (record.getThrown() != null) {
            /*log += "\n" + record.getThrown().getMessage();
            
            for (StackTraceElement element : record.getThrown().getStackTrace()) {
                log += "\n" + element.toString();
            }*/
            log += '\n' + ExceptionUtils.getStackTrace(record.getThrown());
        }

        log += System.getProperty("line.separator");

        return log;
    }
}