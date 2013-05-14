/*
 * Written by Peter Levart <peter.levart@gmail.com>
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

/**
 * @author peter
 */
public class StringContentEqualsBug {

    static class Task extends Thread {
        volatile StringBuffer sb;
        volatile Exception exception;
        int cnt;

        Task(StringBuffer sb) {
            this.sb = sb;
        }

        @Override
        public void run() {
            int cnt = 0;
            try {
                StringBuffer sb;
                while ((sb = this.sb) != null) {
                    if ("QQ".contentEquals(sb))
                        cnt++;
                    else
                        cnt--;
                    sb.setLength(0);
                    sb.trimToSize();
                    sb.append("AA");
                }
            }
            catch (Exception e) {
                exception = e;
            }
            this.cnt = cnt;
        }
    }

    public static void main(String[] args) throws Exception {
        StringBuffer sb = new StringBuffer();
        Task[] tasks = new Task[2];
        for (int i = 0; i < tasks.length; i++) {
            (tasks[i] = new Task(sb)).start();
        }
        try
        {
            // wait at most 10 seconds for any of the threads to throw exception
            for (int i = 0; i < 40; i++) {
                for (Task task : tasks) {
                    if (task.exception != null) {
                        throw task.exception;
                    }
                }
                Thread.sleep(250L);
            }
        }
        finally {
            for (Task task : tasks) {
                task.sb = null;
                task.join();
            }
        }
    }
}
