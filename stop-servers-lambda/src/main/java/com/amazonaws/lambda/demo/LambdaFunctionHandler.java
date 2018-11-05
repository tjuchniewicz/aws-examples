package com.amazonaws.lambda.demo;

import java.util.Arrays;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class LambdaFunctionHandler implements RequestHandler<Object, String> {

	@Override
	public String handleRequest(Object input, Context context) {
		context.getLogger().log("Input: " + input + "\n");

		AmazonEC2 client = AmazonEC2ClientBuilder.defaultClient();

		StringBuilder result = new StringBuilder();
		boolean done = false;

		while (!done) {
			DescribeInstancesRequest request = new DescribeInstancesRequest()
					.withFilters(new Filter("tag:Type").withValues("dev"));
			DescribeInstancesResult response = client.describeInstances(request);

			for (Reservation reservation : response.getReservations()) {
				for (Instance instance : reservation.getInstances()) {
					context.getLogger()
							.log(String.format(
									"Found reservation with id %s, " + "AMI %s, " + "type %s, " + "state %s "
											+ "and monitoring state %s\n",
									instance.getInstanceId(), instance.getImageId(), instance.getInstanceType(),
									instance.getState().getName(), instance.getMonitoring().getState()));

					StopInstancesRequest stopInstancesRequest = new StopInstancesRequest(
							Arrays.asList(instance.getInstanceId()));
					StopInstancesResult stopInstancesResult = client.stopInstances(stopInstancesRequest);
					result.append(stopInstancesResult.toString()).append("\n");
				}
			}

			request.setNextToken(response.getNextToken());

			if (response.getNextToken() == null) {
				done = true;
			}
		}

		return result.toString();
	}

}
