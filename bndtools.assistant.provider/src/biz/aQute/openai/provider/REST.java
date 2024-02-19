package biz.aQute.openai.provider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import aQute.lib.io.IO;
import biz.aQute.openai.api.OpenAI.BaseAPI;
import biz.aQute.openai.assistant.api.Message.MessageDTO;
import biz.aQute.openai.provider.OpenAIProvider.BaseImpl;
import biz.aQute.openai.provider.OpenAIProvider.Client;
import biz.aQute.openai.provider.OpenAIProvider.DeletionStatus;
import biz.aQute.openai.provider.OpenAIProvider.ListDTO;

@SuppressWarnings("unchecked")
class REST<T extends BaseImpl, I extends BaseAPI> implements AutoCloseable {
	final Class<T>							target;
	final String							url;
	final Map<String, T>					assets	= new ConcurrentHashMap<>();
	final Function<Map<String, Object>, T>	create;
	final Client							client;
	final Map<String, String>				headers;
	final ScheduledExecutorService			scheduler;
	volatile String							lastId;
	volatile AutoCloseable					poller;
	volatile Predicate<I>					invalid	= x -> false;

	REST(ScheduledExecutorService s, Client client, Map<String, String> headers, String url, Class<T> target,
		Class<I> interfce, Function<Map<String, Object>, T> create) {
		this.scheduler = s;
		this.headers = headers;
		this.client = client;
		this.url = url;
		this.target = target;
		this.create = create;
	}

	List<I> list() {
		ListDTO list = client.webrequest(url, "GET", headers, null, ListDTO.class);
		List<T> result = list.data.stream()
			.map(this::map)
			.toList();
		return (List<I>) result;
	}

	List<I> list(Integer limit, String before, String after) {
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		String del = "?order=asc&";
		if (limit != null) {
			sb.append(del)
				.append("limit=")
				.append(limit);
			del = "&";
		}
		if (before != null) {
			sb.append(del)
				.append("before=")
				.append(before);
			del = "&";
		}
		if (after != null) {
			sb.append(del)
				.append("after=")
				.append(after);
			del = "&";
		}

		ListDTO list = client.webrequest(sb.toString(), "GET", headers, null, ListDTO.class);
		List<T> result = list.data.stream()
			.map(this::map)
			.toList();
		return (List<I>) result;
	}

	T create(Map<String, Object> pars) {
		Map<String, Object> dto = client.webrequest(url, "POST", headers, pars, Map.class);
		return map(dto);
	}

	T modify(String id, Map<String, Object> pars) {
		Map<String, Object> dto = client.webrequest(url + id, "POST", headers, pars, Map.class);
		return map(dto);
	}

	T get(String id) {
		Map<String, Object> dto = client.webrequest(url + "/" + id, "GET", headers, null, Map.class);
		return map(dto);
	}

	boolean delete(T target) {
		DeletionStatus dto = client.webrequest(url + "/" + target.getId(), "DELETE", headers, null,
			DeletionStatus.class);
		assets.remove(dto.id);
		return dto.deleted;
	}

	T map(Map<String, Object> dto) {
		String id = (String) dto.get("id");
		T target = assets.compute(id, (k, v) -> {
			if (v == null)
				return create.apply(dto);
			v.setDTO(dto);
			return v;
		});
		return target;
	}

	Set<String>	ids	= new HashSet<>();
	MessageDTO	dto;

	AutoCloseable poll(Consumer<I> received, long period) {
		ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(() -> {
			try {
				List<I> list = new ArrayList<>(list(null, null, lastId));
				list.removeIf(invalid);
				if (!list.isEmpty()) {
					list.forEach(m -> {
						received.accept(m);
					});
					lastId = list.get(list.size() - 1)
						.getId();
				}
				try {
					Thread.sleep(period);
				} catch (InterruptedException e) {
					Thread.currentThread()
						.interrupt();
					System.out.println("terminating poll for " + url);
					return;
				}
			} catch (Exception e) {
				if (e instanceof InterruptedException) {
					System.out.println("terminated " + url);
					return;
				}
				e.printStackTrace();
			}
		}, period, period, TimeUnit.MILLISECONDS);
		return () -> {
			future.cancel(true);
		};
	}

	final List<Consumer<I>> listeners = new CopyOnWriteArrayList<>();

	public synchronized AutoCloseable onReceive(Consumer<I> received) {
		listeners.add(received);
		if (poller == null) {
			poller = poll(m -> {
				listeners.forEach(r -> r.accept(m));
			}, 500);
		}
		return () -> listeners.remove(received);
	}

	public void setValidator(Predicate<I> validator) {
		invalid = validator.negate();
	}

	@Override
	public void close() {
		IO.close(poller);
	}
}
