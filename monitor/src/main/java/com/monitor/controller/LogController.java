package com.monitor.controller;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class LogController {

    private final ElasticsearchClient esClient;

    public LogController() {
        // Simple manual config for now
        RestClient restClient = RestClient.builder(
                new HttpHost("localhost", 9200)).build();
        RestClientTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper());
        this.esClient = new ElasticsearchClient(transport);
    }

    @GetMapping("/logs")
    public String viewLogs(@RequestParam(required = false, defaultValue = "") String query, Model model)
            throws IOException {
        // Search index "app-logs-*"
        // Sort by @timestamp desc

        SearchResponse<ObjectNode> response = esClient.search(s -> s
                .index("app-logs-*")
                .size(100)
                .sort(so -> so.field(
                        f -> f.field("@timestamp").order(co.elastic.clients.elasticsearch._types.SortOrder.Desc))),
                ObjectNode.class);

        List<Map<String, Object>> logs = new ArrayList<>();
        for (Hit<ObjectNode> hit : response.hits().hits()) {
            if (hit.source() != null) {
                // Convert Jackson ObjectNode to Map
                logs.add(new com.fasterxml.jackson.databind.ObjectMapper().convertValue(hit.source(), Map.class));
            }
        }

        model.addAttribute("logs", logs);
        return "monitor";
    }
}
