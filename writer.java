import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;


public class writer{
	public static void main(String[] args){
		Charset utf8 = StandardCharsets.UTF_8;
		List<String> lines = Arrays.asList("The first line", "The second line");
		try{
			Files.write(Paths.get("/tmp/coprhd-controller/test.txt"), lines, Charset.forName("UTF-8"));
		} catch (IOException e){
			System.out.println("Error");
		}
	}
}
