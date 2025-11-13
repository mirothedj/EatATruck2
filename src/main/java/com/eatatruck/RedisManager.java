package com.eatatruck;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RedisManager {

    private final JedisPool jedisPool;
    private final Gson gson;

    private static final String MENU_ITEMS_KEY = "menu:items";
    private static final String CAMPAIGNS_KEY = "campaigns";
    private static final String CONVERSATION_STATE_KEY_PREFIX = "conversation:";
    private static final String CONVERSATION_STATE_SUFFIX = ":state";
    private static final String CONVERSATION_DATA_SUFFIX = ":data";

    public RedisManager(String host, int port) {
        this.jedisPool = new JedisPool(host, port);
        // This custom adapter is needed for Gson to handle LocalDateTime
        this.gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();
    }

    public void close() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }

    // --- Menu Item Methods ---

    public void saveMenuItem(MenuItem item) {
        try (Jedis jedis = jedisPool.getResource()) {
            String itemJson = gson.toJson(item);
            jedis.hset(MENU_ITEMS_KEY, item.id(), itemJson);
        }
    }

    public MenuItem getMenuItem(String itemId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String itemJson = jedis.hget(MENU_ITEMS_KEY, itemId);
            if (itemJson == null) {
                return null;
            }
            return gson.fromJson(itemJson, MenuItem.class);
        }
    }

    public List<MenuItem> getAllMenuItems() {
        try (Jedis jedis = jedisPool.getResource()) {
            Map<String, String> itemsJson = jedis.hgetAll(MENU_ITEMS_KEY);
            return itemsJson.values().stream()
                    .map(json -> gson.fromJson(json, MenuItem.class))
                    .collect(Collectors.toList());
        }
    }

    public void deleteMenuItem(String itemId) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hdel(MENU_ITEMS_KEY, itemId);
        }
    }

    // --- Campaign Methods ---

    public void saveCampaign(Campaign campaign) {
        try (Jedis jedis = jedisPool.getResource()) {
            String campaignJson = gson.toJson(campaign);
            jedis.hset(CAMPAIGNS_KEY, campaign.id(), campaignJson);
        }
    }

    public Campaign getCampaign(String campaignId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String campaignJson = jedis.hget(CAMPAIGNS_KEY, campaignId);
            if (campaignJson == null) {
                return null;
            }
            return gson.fromJson(campaignJson, Campaign.class);
        }
    }

    public List<Campaign> getAllCampaigns() {
        try (Jedis jedis = jedisPool.getResource()) {
            Map<String, String> campaignsJson = jedis.hgetAll(CAMPAIGNS_KEY);
            return campaignsJson.values().stream()
                    .map(json -> gson.fromJson(json, Campaign.class))
                    .collect(Collectors.toList());
        }
    }

    public void deleteCampaign(String campaignId) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hdel(CAMPAIGNS_KEY, campaignId);
        }
    }

    // --- Conversation State Methods ---

    private String getConversationStateKey(String ownerPhone) {
        return CONVERSATION_STATE_KEY_PREFIX + ownerPhone + CONVERSATION_STATE_SUFFIX;
    }

    private String getConversationDataKey(String ownerPhone) {
        return CONVERSATION_STATE_KEY_PREFIX + ownerPhone + CONVERSATION_DATA_SUFFIX;
    }

    public String getConversationState(String ownerPhone) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get(getConversationStateKey(ownerPhone));
        }
    }

    public void setConversationState(String ownerPhone, String state, int timeoutSeconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = getConversationStateKey(ownerPhone);
            jedis.setex(key, timeoutSeconds, state);
        }
    }

    public Map<String, String> getConversationData(String ownerPhone) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.hgetAll(getConversationDataKey(ownerPhone));
        }
    }

    public String getConversationData(String ownerPhone, String field) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.hget(getConversationDataKey(ownerPhone), field);
        }
    }

    public void setConversationData(String ownerPhone, String field, String value, int timeoutSeconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = getConversationDataKey(ownerPhone);
            jedis.hset(key, field, value);
            jedis.expire(key, timeoutSeconds);
        }
    }

    public void deleteConversationState(String ownerPhone) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(getConversationStateKey(ownerPhone));
            jedis.del(getConversationDataKey(ownerPhone));
        }
    }
}
