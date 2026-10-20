import java.util.HashMap;
import java.util.Map;

/** Full names for state codes, so pages can say "in New York" instead of "in NY". */
final class StateNames {
    private static final Map<String, String> NAMES = new HashMap<>();
    static {
        String[] pairs = ("AL Alabama,AK Alaska,AZ Arizona,AR Arkansas,CA California,CO Colorado,CT Connecticut,DE Delaware,"
            + "DC District of Columbia,FL Florida,GA Georgia,HI Hawaii,ID Idaho,IL Illinois,IN Indiana,IA Iowa,KS Kansas,"
            + "KY Kentucky,LA Louisiana,ME Maine,MD Maryland,MA Massachusetts,MI Michigan,MN Minnesota,MS Mississippi,"
            + "MO Missouri,MT Montana,NE Nebraska,NV Nevada,NH New Hampshire,NJ New Jersey,NM New Mexico,NY New York,"
            + "NC North Carolina,ND North Dakota,OH Ohio,OK Oklahoma,OR Oregon,PA Pennsylvania,RI Rhode Island,"
            + "SC South Carolina,SD South Dakota,TN Tennessee,TX Texas,UT Utah,VT Vermont,VA Virginia,WA Washington,"
            + "WV West Virginia,WI Wisconsin,WY Wyoming,PR Puerto Rico,GU Guam,VI U.S. Virgin Islands,AS American Samoa,"
            + "MP Northern Mariana Islands,FM Micronesia,MH Marshall Islands,PW Palau").split(",");
        for (String p : pairs) NAMES.put(p.substring(0, 2), p.substring(3));
    }
    private StateNames() { }
    static String name(String code) { return NAMES.getOrDefault(code, code); }
}
