package com.amazonaws.lambda.demo;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClientBuilder;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.Filter;
import com.amazonaws.services.rds.model.StopDBInstanceRequest;

public class StopDbLambdaFunctionHandler implements RequestHandler<Object, String> {

	@Override
	public String handleRequest(Object input, Context context) {
		context.getLogger().log("Input: " + input + "\n");

		AmazonRDS client = AmazonRDSClientBuilder.defaultClient();

		StringBuilder result = new StringBuilder();
		boolean done = false;

		while (!done) {
			DescribeDBInstancesRequest request = new DescribeDBInstancesRequest()
					.withFilters(new Filter().withName("db-instance-id").withValues("postgres-db-1"));
			DescribeDBInstancesResult response = client.describeDBInstances(request);

			for (DBInstance instance : response.getDBInstances()) {
				context.getLogger()
						.log(String.format(
								"Found db instance with id %s, " + "type %s, " + "state %s",
								instance.getDBInstanceIdentifier(), instance.getEngine(),
								instance.getStatusInfos()));

				StopDBInstanceRequest stopInstancesRequest = new StopDBInstanceRequest()
						.withDBInstanceIdentifier(instance.getDBInstanceIdentifier());
				DBInstance stopDBInstance = client.stopDBInstance(stopInstancesRequest);
				result.append(stopDBInstance.toString()).append("\n");
			}

			request.setMarker(response.getMarker());

			if (response.getMarker() == null) {
				done = true;
			}
		}

		return result.toString();
	}

}
