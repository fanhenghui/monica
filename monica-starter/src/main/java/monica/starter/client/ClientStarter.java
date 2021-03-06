package monica.starter.client;

import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;
import monica.configuration.context.ConfigurationContext;
import monica.coordinator.impl.ServiceBuilder;
import monica.framework.Client;
import monica.registry.base.RegistryType;
import monica.registry.base.UriSpec;
import monica.registry.centre.RegistryCentre;
import monica.registry.context.RegistryContext;
import monica.registry.service.ZookeeperMonicaClient;

/**
 * 
 * @author lucy@polarcoral.com
 *
 *         2017-08-29
 */
public class ClientStarter {
	private final Logger log = LoggerFactory.getLogger(getClass());
	private final String SERVER_IP_CACHE = "server_ip";
	private final String SERVER_PORT_CACHE = "server_port";
	private final String SOCKET_CHANNEL = "channel"; 

	public void start() throws Exception {
		ConfigurationContext.loadYamlClientConfig();
		new ZookeeperMonicaClient().start();
		new ServiceBuilder().servicesInit().route().loadbalance().build();
		String ip = (String) RegistryContext.clientCache.get(SERVER_IP_CACHE);
		int port = Integer.valueOf((String) RegistryContext.clientCache.get(SERVER_PORT_CACHE));
		Client client = ClientFactory.newFactory().getConsumerClient();	
		Executors.newSingleThreadExecutor().execute(new NettyRunnable(client, ip, port));
		for (;;) {
			if (client.isStarted()) {
				new RegistryCentre().setUri(createUri()).setType(RegistryType.CLIENT).start();
				if (RegistryCentre.registryFinished()) {
					log.info("monica client start successfully!");
				}
				break;
			}
		}

	}

	// register the  consumer
	private UriSpec createUri() {
		return new UriSpec();
	}
	
	
	public Channel getSocketChannel(){
		return (Channel)ConfigurationContext.propMap.get(SOCKET_CHANNEL);
	}

	public static void main(String args[]) {
		ClientStarter container = new ClientStarter();
		try {
			container.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

class NettyRunnable implements Runnable {
	private Client client;
	private String ip;
	private int port;

	public NettyRunnable(Client monicaClient, String clientIp, int clientPort) {
		client = monicaClient;
		ip = clientIp;
		port = clientPort;
	}

	public void run() {
		try {
			client.start(ip, port);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
