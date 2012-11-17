/*
 * Copyright 2012 ZerothAngel <zerothangel@tyrannyofheaven.org>
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
package org.tyrannyofheaven.bukkit.zPermissions;

import java.util.LinkedList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.MessagePrompt;
import org.bukkit.conversations.Prompt;
import org.bukkit.util.ChatPaginator;

public class PagerPrompt implements Prompt {

    private static final int DEFAULT_LINES_PER_PAGE = ChatPaginator.CLOSED_CHAT_PAGE_HEIGHT - 2; // Room for header and prompt

    private final List<String> lines = new LinkedList<String>();

    private final int linesPerPage;

    private final int totalPages;

    private int currentLine;

    private int currentPage;

    private boolean displayHeader;

    private boolean shouldBlock;

    private static final Prompt ABORTED_PROMPT = new MessagePrompt() {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.YELLOW + "Stopping.";
        }

        @Override
        protected Prompt getNextPrompt(ConversationContext context) {
            return Prompt.END_OF_CONVERSATION;
        }
        
    };

    public PagerPrompt(List<String> lines) {
        this(lines, DEFAULT_LINES_PER_PAGE);
    }

    public PagerPrompt(List<String> lines, int linesPerPage) {
        if (lines == null || lines.isEmpty())
            throw new IllegalArgumentException("lines cannot be empty");
        this.lines.addAll(lines);
        this.linesPerPage = linesPerPage;
        
        totalPages = (lines.size() + linesPerPage - 1) / linesPerPage;
        
        displayHeader = totalPages > 1;
    }

    @Override
    public String getPromptText(ConversationContext context) {
        if (displayHeader) {
            displayHeader = false;
            shouldBlock = false;
            return ChatColor.YELLOW + String.format("-- Page %d of %d --", currentPage + 1, totalPages);
        }
        else if (currentLine < linesPerPage) {
            // Next line
            String prompt = lines.remove(0);
            currentLine++;
            shouldBlock = false;
            return prompt;
        }
        else {
            // Next page
            currentLine = 0;
            currentPage++;
            displayHeader = true;
            shouldBlock = true;
            return ChatColor.YELLOW + "More? y/n";
        }
    }

    @Override
    public boolean blocksForInput(ConversationContext context) {
        return shouldBlock;
    }

    @Override
    public Prompt acceptInput(ConversationContext context, String input) {
        // Sanitize
        if (input != null) {
            input = input.toLowerCase().trim();
            // Only care about first char
            if (!input.isEmpty())
                input = input.substring(0, 1);
        }

        if ("n".equals(input)) {
            return ABORTED_PROMPT;
        }
        else {
            // Not blocking
            return !lines.isEmpty() ? this : Prompt.END_OF_CONVERSATION;
        }
    }

}
