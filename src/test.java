import java.io.File;
import java.io.FileFilter;

public class test {
    public static void print_sub(String path){
        File f=new File(path);
        if(f.isDirectory()){
            for(File s:f.listFiles(new FileFilter() {

                @Override
                public boolean accept(File pathname) {
                    // TODO Auto-generated method stub
                    return true;
                }
                
            })){
                if(s.isDirectory()){
                    print_sub(s.getAbsolutePath());
                }else if(s.getName().contains(".class")){
                    System.err.println(s.getAbsolutePath());
                }
            }
        }
    }
    public static void main(String[] args) {
        print_sub("\\Users\\n" + //
                        "oliw\\OneDrive\\Documents\\builds\\miscellanius\\src\\out");
    }
}
