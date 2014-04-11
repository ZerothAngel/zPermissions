/*
 * Largely based off Mojang's AccountsClient code.
 * https://github.com/Mojang/AccountsClient
 */
package org.tyrannyofheaven.bukkit.zPermissions.uuid;

import static org.tyrannyofheaven.bukkit.util.ToHStringUtils.hasText;
import static org.tyrannyofheaven.bukkit.zPermissions.uuid.UuidUtils.uncanonicalizeUuid;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Charsets;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.Lists;
import com.google.gson.Gson;

public class MojangUuidResolver implements UuidResolver {

    private final Gson gson = new Gson();

    private static final UuidDisplayName NULL_UDN = new UuidDisplayName(UUID.randomUUID(), "NOT FOUND");

    private final Cache<String, UuidDisplayName> cache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(5L, TimeUnit.MINUTES)
            .build(new CacheLoader<String, UuidDisplayName>() {
                @Override
                public UuidDisplayName load(String key) throws Exception {
                    UuidDisplayName udn = _resolve(key);
                    return udn != null ? udn : NULL_UDN; // Doesn't like nulls, so we use a marker object instead
                }
            });

    @Override
    public UuidDisplayName resolve(String username) {
        if (!hasText(username))
            throw new IllegalArgumentException("username must have a value");

        try {
            UuidDisplayName udn = cache.get(username.toLowerCase());
            return udn != NULL_UDN ? udn : null;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Map<String, UuidDisplayName> resolve(Collection<String> usernames) throws IOException {
        Map<String, UuidDisplayName> result = new LinkedHashMap<String, UuidDisplayName>();

        final int BATCH_SIZE = 100;
        final int MAX_PAGES = BATCH_SIZE; // The off chance that it returns 1 per page

        for (List<String> sublist : Lists.partition(new ArrayList<String>(usernames), BATCH_SIZE)) {
            List<ProfileCriteria> criteriaList = new ArrayList<ProfileCriteria>(sublist.size());
            for (String username : sublist) {
                criteriaList.add(new ProfileCriteria(username, "minecraft"));
            }
            ProfileCriteria[] criteria = criteriaList.toArray(new ProfileCriteria[criteriaList.size()]);

            for (int page = 1; page <= MAX_PAGES; page++) {
                ProfileSearchResult searchResult = searchProfiles(page, criteria);
                if (searchResult.getSize() == 0) break;
                for (int i = 0; i < searchResult.getSize(); i++) {
                    String username = searchResult.getProfiles()[i].getName();
                    UUID uuid = uncanonicalizeUuid(searchResult.getProfiles()[i].getId());
                    result.put(username.toLowerCase(), new UuidDisplayName(uuid, username));
                }
            }
        }

        return result;
    }

    private UuidDisplayName _resolve(String username) throws IOException {
        if (!hasText(username))
            throw new IllegalArgumentException("username must have a value");

        ProfileSearchResult result = searchProfiles(1, new ProfileCriteria(username, "minecraft"));

        if (result.getSize() < 1) return null;

        // TODO what to do if there are >1?
        Profile p = result.getProfiles()[0];

        String uuidString = p.getId();
        UUID uuid;
        try {
            uuid = uncanonicalizeUuid(uuidString);
        }
        catch (IllegalArgumentException e) {
            return null;
        }

        String displayName = hasText(p.getName()) ? p.getName() : username;

        return new UuidDisplayName(uuid, displayName);
    }

    private ProfileSearchResult searchProfiles(int page, ProfileCriteria... criteria) throws MalformedURLException, IOException, ProtocolException {
        String body = gson.toJson(criteria);

        URL url = new URL("https://api.mojang.com/profiles/page/" + page);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

        connection.setUseCaches(false);
        connection.setDoOutput(true);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        
        DataOutputStream writer = new DataOutputStream(connection.getOutputStream());
        try {
            writer.write(body.getBytes(Charsets.UTF_8));
            writer.flush();
        }
        finally {
            writer.close();
        }

        ProfileSearchResult result;

        Reader reader = new InputStreamReader(connection.getInputStream());
        try {
            result = gson.fromJson(reader, ProfileSearchResult.class);
        }
        finally {
            reader.close();
        }
        return result;
    }

    public static class ProfileCriteria {
        
        private final String name;
        
        private final String agent;

        public ProfileCriteria(String name, String agent) {
            this.name = name;
            this.agent = agent;
        }

        public String getName() {
            return name;
        }

        public String getAgent() {
            return agent;
        }
        
    }

    public static class ProfileSearchResult {

        private Profile[] profiles;

        private int size;

        public Profile[] getProfiles() {
            return profiles;
        }

        public void setProfiles(Profile[] profiles) {
            this.profiles = profiles;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

    }

    public static class Profile {
        
        private String id;
        
        private String name;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
        
    }

}
