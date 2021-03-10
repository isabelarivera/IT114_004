public class MyIfElse {
	public static void main(String[] args) {
		System.out.println("Java Exercise [IF...Else]  Exercise [1]");
		int x = 50;
		int y = 50;
		if (x > y) {
			System.out.println("Hello World");
		}
		System.out.println("Java Exercise [IF...Else]  Exercise [2]");
		// int x = 50;
		// int y = 50;
		if (x == y) {
			System.out.println("Hello World");
		}
		System.out.println("Java Exercise [IF...Else]  Exercise [3]");
		// int x = 50;
		// int y = 50;
		if (x == y) {
			System.out.println("Yes");
		} else {
			System.out.println("No");
		}
		System.out.println("Java Exercise [IF...Else]  Exercise [4]");
		// int x = 50;
		// int y = 50;
		if (x == y) {
			System.out.println("1");
		} else if (x > y) {
			System.out.println("2");
		} else {
			System.out.println("3");
		}
		System.out.println("Java Exercise [IF...Else]  Exercise [5]");

		int time = 20;
		String result = (time < 18) ? "Good day." : "Good evening.";
		System.out.println(result);
	}
}
