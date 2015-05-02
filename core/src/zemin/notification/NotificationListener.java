package zemin.notification;

//
// @author Zemin Liu
//
public interface NotificationListener {

    void onArrival(NotificationEntry entry);
    void onCancel(NotificationEntry entry);
}
