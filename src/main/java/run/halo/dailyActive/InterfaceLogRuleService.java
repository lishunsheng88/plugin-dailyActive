package run.halo.dailyActive;

import org.springframework.data.domain.Sort;
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.index.query.QueryFactory;

@Component
public class InterfaceLogRuleService {

    private ReactiveExtensionClient client;

    public InterfaceLogRuleService(ReactiveExtensionClient client) {
        this.client = client;
    }


}
