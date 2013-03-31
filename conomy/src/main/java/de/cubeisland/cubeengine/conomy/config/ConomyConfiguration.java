package de.cubeisland.cubeengine.conomy.config;

import de.cubeisland.cubeengine.core.config.Configuration;
import de.cubeisland.cubeengine.core.config.annotations.Codec;
import de.cubeisland.cubeengine.core.config.annotations.Comment;
import de.cubeisland.cubeengine.core.config.annotations.DefaultConfig;
import de.cubeisland.cubeengine.core.config.annotations.Option;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

@Codec("yml")
@DefaultConfig
public class ConomyConfiguration extends Configuration
{
    @Comment("Keep in mind subcurrencies have to be ordered in order of their value beginning with the highest.")
    @Option("currencies")
    public LinkedHashMap<String, CurrencyConfiguration> currencies = new LinkedHashMap<String, CurrencyConfiguration>();
    @Option("relations")
    public LinkedHashMap<String, Map<String, Double>> relations = new LinkedHashMap<String, Map<String, Double>>();
    @Option("enable-logging")
    public boolean enableLogging = true;

    @Override
    public void onLoaded(File loadFrom)
    {
        // Create a default currency when none given
        if (currencies == null || currencies.isEmpty())
        {
            this.currencies = new LinkedHashMap<String, CurrencyConfiguration>();
            LinkedHashMap<String, SubCurrencyConfig> subcurrencies = new LinkedHashMap<String, SubCurrencyConfig>();
            subcurrencies.put("Euro", new SubCurrencyConfig("Euros","€","€", 1));
            subcurrencies.put("Cent", new SubCurrencyConfig("Cents","c","c", 100));
            CurrencyConfiguration subConfig = new CurrencyConfiguration(subcurrencies, "%-%€ Euro %c Cent", "%-%€,%c€", 10000,0,",",".");
            this.currencies.put("Euro", subConfig);
        }
    }
}