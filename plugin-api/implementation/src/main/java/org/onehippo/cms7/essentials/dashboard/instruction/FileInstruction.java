/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.dashboard.instruction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.io.FileUtils;
import org.apache.wicket.util.string.Strings;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.event.InstructionEvent;
import org.onehippo.cms7.essentials.dashboard.event.MessageEvent;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.TemplateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.name.Named;


/**
 * @version "$Id$"
 */
@XmlRootElement(name = "file", namespace = EssentialConst.URI_ESSENTIALS_INSTRUCTIONS)
public class FileInstruction extends PluginInstruction {

    public static final String COPY = "copy";
    public static final String DELETE = "delete";
    public static final Set<String> VALID_ACTIONS = new ImmutableSet.Builder<String>()
            .add(COPY)
            .add(DELETE)
            .add("overwrite")
            .build();
    private static final Logger log = LoggerFactory.getLogger(FileInstruction.class);
    private String message;

    @Inject
    private EventBus eventBus;
    @Inject
    @Named("instruction.message.file.delete")
    private String messageDelete;


    @Inject
    @Named("instruction.message.file.copy")
    private String messageCopy;

    private boolean override;
    private String source;
    private String target;
    private String action;


    @Override
    public InstructionStatus process(final PluginContext context) {
        log.debug("executing FILE Instruction {}", this);
        if (!valid()) {
            eventBus.post(new MessageEvent("Invalid instruction descriptor: " + toString()));
            return InstructionStatus.FAILED;
        }
        processPlaceholders(context.getPlaceholderData());
        // check action:
        if (action.equals(COPY)) {
            return copy();
        } else if (action.equals(DELETE)) {
            return delete();
        }


        eventBus.post(new InstructionEvent(this));
        return InstructionStatus.FAILED;
    }

    private InstructionStatus copy() {
        final File file = new File(source);
        if (!file.exists()) {
            log.error("Source file doesn't exists: {}", file);
        }
        try {
            FileUtils.copyFile(file, new File(target));
            eventBus.post(new InstructionEvent(this));
            return InstructionStatus.SUCCESS;
        } catch (IOException e) {
            log.error("Error creating file", e);
        }


        return InstructionStatus.FAILED;

    }

    private InstructionStatus delete() {
        try {
            Path path = new File(target).toPath();
            final boolean deleted = Files.deleteIfExists(path);
            eventBus.post(new InstructionEvent(this));
            if (deleted) {
                return InstructionStatus.SUCCESS;
            } else {
                return InstructionStatus.SKIPPED;
            }
        } catch (IOException e) {
            log.error("Error deleting file", e);
        }
        return InstructionStatus.FAILED;
    }

    @Override
    public void processPlaceholders(final Map<String, Object> data) {
        // setup messages:
        if (Strings.isEmpty(message)) {
            // check message based on action:
            if (action.equals(COPY)) {
                message = messageCopy;
            } else if (action.equals(DELETE)) {
                message = messageDelete;
            }
        }

        super.processPlaceholders(data);
        //
        final String myTarget = TemplateUtils.replaceTemplateData(target, data);
        if (myTarget != null) {
            target = myTarget;
        }
        //
        final String mySource = TemplateUtils.replaceTemplateData(source, data);
        if (mySource != null) {
            source = mySource;
        }
    }

    private boolean valid() {
        if (Strings.isEmpty(action) || !VALID_ACTIONS.contains(action) || Strings.isEmpty(target)) {
            return false;
        }
        return true;
    }

    @XmlAttribute
    public boolean isOverride() {
        return override;
    }

    public void setOverride(final boolean override) {
        this.override = override;
    }

    @XmlAttribute
    public String getSource() {
        return source;
    }

    public void setSource(final String source) {
        this.source = source;
    }

    @XmlAttribute
    public String getTarget() {
        return target;
    }

    public void setTarget(final String target) {
        this.target = target;
    }

    @XmlAttribute
    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public void setMessage(final String message) {
        this.message = message;
    }

    @XmlAttribute
    @Override
    public String getAction() {
        return action;
    }

    @Override
    public void setAction(final String action) {
        this.action = action;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("FileInstruction{");
        sb.append("message='").append(message).append('\'');
        sb.append(", override=").append(override);
        sb.append(", source='").append(source).append('\'');
        sb.append(", target='").append(target).append('\'');
        sb.append(", action='").append(action).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
