package com.amazonaws.lambda.demo;

import java.io.IOException;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.PutMetricDataResult;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class CustomMetricsHandler implements RequestHandler<Object, String> {

	@Override
	public String handleRequest(Object input, Context context) {
		final AmazonCloudWatch cw = AmazonCloudWatchClientBuilder.defaultClient();

		Dimension dimension = new Dimension().withName("TEST").withValue("LIVEBOX_STATS");

		String url = System.getenv("URL");
		String username = System.getenv("USERNAME");
		String password = System.getenv("PASSWORD");
		
		LiveboxMonitor liveboxMonitor = new LiveboxMonitor(url, username, password);
		double downStreamCurrRateValue = 0;
		double upstreamCurrRateValue = 0;
		try {
			liveboxMonitor.connect();
			downStreamCurrRateValue = liveboxMonitor.getDownStreamCurrRate();
			upstreamCurrRateValue = liveboxMonitor.getUpstreamCurrRate();
		} catch (Exception e) {
			// ignore
		} finally {
			try {
				liveboxMonitor.disconnect();
			} catch (IOException e) {
				// ignore
			}
		}
		context.getLogger().log("DownStreamCurrRate: " + downStreamCurrRateValue);
		context.getLogger().log("UpstreamCurrRate: " + upstreamCurrRateValue);
		MetricDatum datum1 = new MetricDatum()
				.withMetricName("DownStreamCurrRate")
				.withUnit(StandardUnit.BitsSecond)
				.withValue(downStreamCurrRateValue)
				.withDimensions(dimension);
		
		MetricDatum datum2 = new MetricDatum()
				.withMetricName("UpstreamCurrRate")
				.withUnit(StandardUnit.BitsSecond)
				.withValue(upstreamCurrRateValue)
				.withDimensions(dimension);

		PutMetricDataRequest request = new PutMetricDataRequest()
				.withNamespace("TEST/METRICS")
				.withMetricData(datum1, datum2);

		PutMetricDataResult response = cw.putMetricData(request);
		context.getLogger().log("Response: " + response.toString());

		return "DownStreamCurrRate: " + downStreamCurrRateValue;
	}

}
