package com.ahmadrezagh671.finxel.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ahmadrezagh671.finxel.R;
import com.ahmadrezagh671.finxel.models.configModel.ConfigModel;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RecyclerView adapter for displaying a list of configuration items
 * with their metadata including provider, field counts, and layout info.
 */
public class RVConfigAdapter extends RecyclerView.Adapter<RVConfigAdapter.ConfigViewHolder>{

    private final List<ConfigEntry> configEntries;
    private final OnItemClick onItemClick;

    public interface OnItemClick {
        void itemClicked(View view,String key, ConfigModel config, int position);
    }

    public RVConfigAdapter(Map<String, ConfigModel> configs,OnItemClick onItemClick) {
        this.onItemClick = onItemClick;
        this.configEntries = new ArrayList<>();

        if (configs != null) {
            List<String> keys = new ArrayList<>(configs.keySet());

            for (String key : keys) {
                ConfigModel config = configs.get(key);
                if (config != null) {
                    configEntries.add(new ConfigEntry(key, config));
                }
            }
        }
    }

    /**
     * Creates a new view holder for a configuration item.
     */
    @NonNull
    @Override
    public RVConfigAdapter.ConfigViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_setting_config, parent, false);
        return new ConfigViewHolder(view);
    }

    /**
     * Binds configuration data to the view holder at the given position.
     *
     * @param holder   the view holder to bind
     * @param position the position of the item in the list
     */
    @Override
    public void onBindViewHolder(@NonNull RVConfigAdapter.ConfigViewHolder holder, int position) {
        ConfigEntry entry = configEntries.get(position);
        ConfigModel config = entry.config;

        holder.tvTitle.setText(config.information.name);
        holder.tvTitle.setTextColor(config.information.color);
        holder.tvProvider.setText("Provider: " + getProviderText(config));
        holder.tvMeta.setText(getMetaText(config));

        holder.cardViewMain.setOnClickListener(v -> {
            onItemClick.itemClicked(v,entry.key, config, position);
        });
    }

    /**
     * Returns the number of configuration items in the list.
     */
    @Override
    public int getItemCount() {
        return configEntries.size();
    }

    /**
     * Returns a display string for the provider field of a config.
     *
     * @param config the config model
     * @return the provider text or "No provider" if empty
     */
    private String getProviderText(ConfigModel config) {
        if (config == null || config.information == null || config.information.provider == null || config.information.provider.isEmpty()) {
            return "No provider";
        }

        return TextUtils.join(", ", config.information.provider);
    }

    /**
     * Returns a metadata summary string for the given config including
     * field counts, layout items, result count, location status, notification
     * entry count, skip count, and compare-last-message count.
     *
     * @param config the config model
     * @return a formatted metadata string
     */
    private String getMetaText(ConfigModel config) {
        if (config == null) {
            return "";
        }

        int fields = config.fields == null ? 0 : config.fields.size();
        int fieldsBeforeClick = config.fieldsBeforeClick == null ? 0 : config.fieldsBeforeClick.size();
        int layoutItems = config.layout == null ? 0 : config.layout.size();
        boolean location = config.information.location;
        int skipsCount = config.skips == null ? 0 : config.skips.size();
        int resultCount = config.result == null ? 0 : config.result.get(0).size();
        int compareLastMessageCount = config.compareWithLastMessage == null ? 0 : config.compareWithLastMessage.size();
        int notificationEntryCount = config.notificationEntries == null ? 0 : config.notificationEntries.size();
        String configVersion = config.information.appConfigVersion;

        return "Fields: " + (fields+fieldsBeforeClick) + "   |   Layout items: " + layoutItems + "   |   Result: " + resultCount
                + "\n" +
                "Location: " + String.valueOf(location).toUpperCase() + "   |   Notif Entry: " + notificationEntryCount + "   |   Skips: " + skipsCount
                + "\n" +
                "Compare Last Message: " + compareLastMessageCount + "   |   C Ver: " + configVersion;
    }

    public static class ConfigViewHolder extends RecyclerView.ViewHolder {

        MaterialCardView cardViewMain;
        TextView tvTitle;
        TextView tvProvider;
        TextView tvMeta;

        public ConfigViewHolder(@NonNull View itemView) {
            super(itemView);
            cardViewMain = itemView.findViewById(R.id.cardViewMain);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvProvider = itemView.findViewById(R.id.tvProvider);
            tvMeta = itemView.findViewById(R.id.tvMeta);
        }
    }

    private static class ConfigEntry {
        String key;
        ConfigModel config;

        ConfigEntry(String key, ConfigModel config) {
            this.key = key;
            this.config = config;
        }
    }
}
