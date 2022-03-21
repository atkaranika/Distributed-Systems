/*
    A class for the main function of group manager
*/


public class Group_manager_app {
    public static void main(String[] args) throws Exception {
        Group_manager_api manager = new Group_manager_api();
        System.out.println("Group Manager start....");
        manager.init_manager();
    }
}