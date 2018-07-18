package net.ukr.shyevhen;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Main {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		try {
			String login = authorization(scanner);
			if (login == null) {
				return;
			}
			Thread th = new Thread(new GetThread(login));
			th.setDaemon(true);
			th.start();
			System.out.println("To open the hepl input -help in the field 'To'");
			inputMessage(login, scanner);
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			scanner.close();
		}
	}

	private static void inputMessage(String login, Scanner scanner) throws IOException {
		while (true) {
			System.out.println("To (for all -> Enter):");
			String to = scanner.nextLine();
			if (to.isEmpty()) {
				to = "All";
			} else if (to.equals("-exit")) {
				System.out.println("Chat closed");
				return;
			} else if (to.startsWith("-")) {
				checkTo(to, login, scanner);
				continue;
			}
			System.out.println("Enter your message: ");
			String text = scanner.nextLine();
			if (text.isEmpty())
				continue;
			Message m = new Message(login, to, text);
			int res = m.send(Utils.getURL() + "/add");
			if (res != 200) { // 200 OK
				System.out.println("HTTP error occured: " + res);
				if (res != 400) {
					return;
				}
			}
		}
	}

	private static void checkTo(String to, String login, Scanner scanner) throws IOException {
		if (to.equals("-help")) {
			getHelp();
		} else if (to.equals("-list")) {
			getList(scanner);
		} else if (to.equals("-chr")) {
			getChatRoom(scanner, login);
		}
	}

	private static String authorization(Scanner scanner) {
		try {
			while (true) {
				System.out.println("From registration input -r");
				System.out.println("Enter your login: ");
				String login = scanner.nextLine();
				if (login.equals("-r")) {
					login = registration(scanner);
					return login;
				}
				System.out.println("Enter your password: ");
				String password = scanner.nextLine();
				if (login.length() < 4 || password.length() < 5 || login.contains(" ") || password.contains(" ")) {
					System.out.println("Wrong login or password format");
					continue;
				}
				String resp = sendUserData(login, password, "no");
				if (!resp.equals("author-OK")) {
					System.out.println("Wrong login or password");
					continue;
				}
				return login;
			}
		} catch (IOException e) {
			System.out.println(e);
			return null;
		}
	}

	private static String registration(Scanner scanner) {
		try {
			while (true) {
				System.out.println("Enter your login: ");
				String login = scanner.nextLine();
				System.out.println("Enter your password: ");
				String password = scanner.nextLine();
				if (login.length() < 3 || password.length() < 5 || login.contains(" ") || password.contains(" ")) {
					System.out.println("Wrong login or password.");
					continue;
				}
				System.out.println("Repeat your password: ");
				String passwordTwo = scanner.nextLine();
				if (!password.equals(passwordTwo)) {
					System.out.println("Passwords don't match");
					continue;
				}
				String resp = sendUserData(login, password, "yes");
				if (!resp.equals("registr-OK")) {
					System.out.println("This login already exists");
					continue;
				}
				return login;
			}
		} catch (IOException e) {
			return null;
		}
	}

	private static String sendUserData(String login, String password, String regist) throws IOException {
		URL url = new URL(
				Utils.getURL() + "/login?login=" + login + "&password=" + password + "&registration=" + regist);
		HttpURLConnection http = (HttpURLConnection) url.openConnection();
		InputStream is = http.getInputStream();
		String resp = new String(requestBodyToArray(is), StandardCharsets.UTF_8);
		is.close();
		return resp;
	}

	private static byte[] requestBodyToArray(InputStream is) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buf = new byte[10240];
		int r;
		do {
			r = is.read(buf);
			if (r > 0)
				bos.write(buf, 0, r);
		} while (r != -1);

		return bos.toByteArray();
	}

	private static void getHelp() throws IOException {
		InputStream is = null;
		try {
			URL url = new URL(Utils.getURL() + "/help");
			HttpURLConnection http = (HttpURLConnection) url.openConnection();
			is = http.getInputStream();
			String resp = new String(requestBodyToArray(is), StandardCharsets.UTF_8);
			System.out.println(resp);
		} catch (IOException e) {
			System.out.println(e);
		} finally {
			is.close();
		}

	}

	private static void getList(Scanner scanner) throws IOException {
		System.out.println("For all list press Enter or input 'online' for online list:");
		String online = scanner.nextLine();
		if (online.equals("online")) {
			online = "/add?online=yes";
		} else {
			online = "/add?online=no";
		}
		InputStream is = null;
		try {
			URL url = new URL(Utils.getURL() + online);
			HttpURLConnection http = (HttpURLConnection) url.openConnection();
			is = http.getInputStream();
			String resp = new String(requestBodyToArray(is), StandardCharsets.UTF_8);
			System.out.println();
			System.out.println(resp);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			is.close();
		}
	}

	private static void getChatRoom(Scanner scanner, String login) throws IOException {
		System.out.println("Input the command in format-> -chr:'command'");
		String com = scanner.nextLine();
		if (com.equals("-chr:new") || com.equals("-chr:exit") || com.equals("-chr:users")) {
			System.out.println("Input chat-room name in format: {name}-chr");
			String name = scanner.nextLine();
			if (name.isEmpty() || !name.endsWith("-chr")) {
				System.out.println("Wrong chat-room name");
				return;
			}
			sendChatRoomData(new URL(Utils.getURL() + "/chatroom?command=" + com + "&login=" + login + "&name=" + name));
		} else if (com.equals("-chr:add")) {
			System.out.println("Input chat-room name");
			String name = scanner.nextLine();
			System.out.println("Input user name:");
			String user = scanner.nextLine();
			if (name.isEmpty() || !name.endsWith("-chr") || user.isEmpty()) {
				System.out.println("Wrong chat-room or user name");
				return;
			}
			sendChatRoomData(new URL(Utils.getURL() + "/chatroom?command=" + com + "&login=" + login + "&name=" + name + "&user="
					+ user));
		} else if (com.equals("-chr:list")) {
			sendChatRoomData(new URL(Utils.getURL() + "/chatroom?command=" + com + "&login=" + login));
		} else {
			System.out.println("Unknown command");
			return;
		}
	}

	private static void sendChatRoomData(URL url) throws IOException {
		InputStream is = null;
		try {
			HttpURLConnection http = (HttpURLConnection) url.openConnection();
			is = http.getInputStream();
			String resp = new String(requestBodyToArray(is), StandardCharsets.UTF_8);
			System.out.println(resp);
		} catch (IOException e) {
			System.out.println(e);
		} finally {
			is.close();
		}
	}
}
