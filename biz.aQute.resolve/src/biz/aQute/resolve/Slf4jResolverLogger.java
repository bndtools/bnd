package biz.aQute.resolve;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jResolverLogger extends ResolverLogger {

	private static final Logger logger = LoggerFactory.getLogger(Slf4jResolverLogger.class);

	@Override
	public void log(int level, String msg, Throwable throwable) {
		super.log(level, msg, throwable);
		if (throwable == null) {
			switch (level) {
				case LOG_DEBUG :
					logger.debug(msg);
					break;
				case LOG_ERROR :
					logger.error(msg);
					break;
				case LOG_INFO :
					logger.info(msg);
					break;
				case LOG_WARNING :
					logger.warn(msg);
					break;
				default :
					logger.warn("Unknown log level {}. Log message was {}", level, msg);
					break;
			}
		} else {
			switch (level) {
				case LOG_DEBUG :
					logger.debug(msg, throwable);
					break;
				case LOG_ERROR :
					logger.error(msg, throwable);
					break;
				case LOG_INFO :
					logger.info(msg, throwable);
					break;
				case LOG_WARNING :
					logger.warn(msg, throwable);
					break;
				default :
					logger.warn("Unknown log level {}. Log message was {}", level, msg, throwable);
					break;
			}
		}
	}
}
