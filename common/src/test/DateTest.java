import me.lucko.luckperms.utils.DateUtil;

public class DateTest {

    public static void main(String[] args) {

        try {
            System.out.println("" + DateUtil.parseDateDiff("1m", true));
            System.out.println(DateUtil.formatDateDiff(DateUtil.parseDateDiff("1 hour, 1 second", true)));

        } catch (DateUtil.IllegalDateException e) {
            e.printStackTrace();
        }

    }

}
