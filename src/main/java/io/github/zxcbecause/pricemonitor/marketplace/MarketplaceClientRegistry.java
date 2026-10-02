package io.github.zxcbecause.pricemonitor.marketplace;

import io.github.zxcbecause.pricemonitor.domain.Marketplace;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MarketplaceClientRegistry {

    private final Map<Marketplace, MarketplaceClient> clients = new EnumMap<>(Marketplace.class);

    public MarketplaceClientRegistry(List<MarketplaceClient> clients) {
        for (MarketplaceClient client : clients) {
            MarketplaceClient previous = this.clients.put(client.marketplace(), client);
            if (previous != null) {
                throw new IllegalStateException("Two clients registered for " + client.marketplace());
            }
        }
    }

    public MarketplaceClient get(Marketplace marketplace) {
        MarketplaceClient client = clients.get(marketplace);
        if (client == null) {
            throw new PriceFetchException("No client for marketplace " + marketplace);
        }
        return client;
    }
}
