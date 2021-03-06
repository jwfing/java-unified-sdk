package cn.leancloud;

import cn.leancloud.callback.FollowersAndFolloweesCallback;
import cn.leancloud.json.JSON;
import cn.leancloud.json.JSONObject;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class UserFollowshipTest extends TestCase {
  private boolean operationSucceed = false;
  public static final String JFENG_EMAIL = "jfeng@test.com";
  public static final String DENNIS_EMAIL = "dennis@test.com";
  public static final String JFENG_001_EMAIL = "jfeng001@test.com";
  public static String DEFAULT_PASSWD = "FER$@$@#Ffwe";

  private static String JFENG_OBJECT_ID = "5bff479067f3560066d00676";
  private static String DENNIS_OBJECT_ID = "5bff452afb4ffe0069a9893e";

  public UserFollowshipTest(String name) {
    super(name);
    Configure.initializeRuntime();
  }

  public static Test suite() {
    return new TestSuite(UserFollowshipTest.class);
  }

  @Override
  protected void setUp() throws Exception {
    try {
      prepareUser("jfeng", JFENG_EMAIL, true);
      prepareUser("dennis", DENNIS_EMAIL, true);
      prepareUser("jfeng001", JFENG_001_EMAIL, false);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    operationSucceed = false;
  }

  @Override
  protected void tearDown() throws Exception {
    ;
  }

  public static void prepareUser(String username, final String email, final boolean loginOnFailed) throws Exception {
    LCUser user = new LCUser();
    user.setEmail(email);
    user.setUsername(username);
    user.setPassword(DEFAULT_PASSWD);
    final CountDownLatch latch = new CountDownLatch(1);
    user.signUpInBackground().subscribe(new Observer<LCUser>() {
      public void onSubscribe(Disposable disposable) {

      }

      public void onNext(LCUser avUser) {
        if (loginOnFailed) {
          if (email.startsWith("jfeng")) {
            JFENG_OBJECT_ID = avUser.getObjectId();
          } else if (email.startsWith("dennis")) {
            DENNIS_OBJECT_ID = avUser.getObjectId();
          }
        }
        latch.countDown();

      }

      public void onError(Throwable throwable) {
        if (loginOnFailed) {
          System.out.println("try to loginWithEmail. cause: " + throwable.getMessage());
          try {
            LCUser tmp = LCUser.loginByEmail(email, DEFAULT_PASSWD).blockingFirst();
            if (email.startsWith("jfeng")) {
              JFENG_OBJECT_ID = tmp.getObjectId();
            } else if (email.startsWith("dennis")) {
              DENNIS_OBJECT_ID = tmp.getObjectId();
            }
          } catch (Exception ex) {
            System.out.println("failed to loginWithEmail. cause: " + ex.getMessage());
          }
        }

        latch.countDown();
      }

      public void onComplete() {

      }
    });
    latch.await();
  }

  public void testFolloweeQuery() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    LCUser.logIn("jfeng", DEFAULT_PASSWD).subscribe(new Observer<LCUser>() {
      public void onSubscribe(Disposable disposable) {
        System.out.println("onSubscribe " + disposable.toString());
      }

      public void onNext(LCUser avUser) {
        System.out.println("onNext. result=" + JSON.toJSONString(avUser));

        LCUser currentUser = LCUser.getCurrentUser();
        System.out.println("currentUser. result=" + JSON.toJSONString(currentUser));
        System.out.println("sessionToken=" + currentUser.getSessionToken() + ", isAuthenticated=" + currentUser.isAuthenticated());

        LCQuery<LCObject> query = avUser.followeeQuery();
        List<LCObject> followees = query.find();
        if (null == followees || followees.size() < 1) {
          avUser.followInBackground(DENNIS_OBJECT_ID).subscribe(new Observer<JSONObject>() {
            @Override
            public void onSubscribe(Disposable disposable) {

            }

            @Override
            public void onNext(JSONObject jsonObject) {
              System.out.println(jsonObject.toJSONString());
              operationSucceed = true;
              latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
              throwable.printStackTrace();
              latch.countDown();
            }

            @Override
            public void onComplete() {

            }
          });

        } else {
          operationSucceed = true;
          latch.countDown();
        }
      }

      public void onError(Throwable throwable) {
        throwable.printStackTrace();
        latch.countDown();
      }

      public void onComplete() {
        System.out.println("onComplete");
      }
    });
    latch.await();
    assertTrue(operationSucceed);
  }

  public void testFollowerQuery() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    LCUser.logIn("jfeng", DEFAULT_PASSWD).subscribe(new Observer<LCUser>() {
      public void onSubscribe(Disposable disposable) {
        System.out.println("onSubscribe " + disposable.toString());
      }

      public void onNext(LCUser avUser) {
        System.out.println("onNext. result=" + JSON.toJSONString(avUser));

        LCUser currentUser = LCUser.getCurrentUser();
        System.out.println("currentUser. result=" + JSON.toJSONString(currentUser));
        System.out.println("sessionToken=" + currentUser.getSessionToken() + ", isAuthenticated=" + currentUser.isAuthenticated());

        LCQuery<LCObject> query = avUser.followerQuery();
        List<LCObject> followers = query.find();
        if (null != followers) {
          for (LCObject fo: followers) {
            System.out.println("follower: " + fo.toJSONString());
          }
        }
        operationSucceed = true;
        latch.countDown();
      }

      public void onError(Throwable throwable) {
        throwable.printStackTrace();
        latch.countDown();
      }

      public void onComplete() {
        System.out.println("onComplete");
      }
    });
    latch.await();
    assertTrue(operationSucceed);
  }

  public void testFollow() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);

    LCUser logginUser = LCUser.logIn("jfeng001", DEFAULT_PASSWD).blockingFirst();
    logginUser.followInBackground(JFENG_OBJECT_ID).subscribe(new Observer<JSONObject>() {
      @Override
      public void onSubscribe(Disposable disposable) {

      }

      @Override
      public void onNext(JSONObject object) {
        System.out.println("succeed follow. " + object.toString());
        LCUser jfeng = LCUser.logIn("jfeng", DEFAULT_PASSWD).blockingFirst();

        LCQuery query = jfeng.followerQuery();
        query.findInBackground().subscribe(new Observer<List<LCObject>>() {
          @Override
          public void onSubscribe(Disposable disposable) {

          }

          @Override
          public void onNext(List<LCObject> o) {
            for (LCObject tmp: o) {
              System.out.println("result User:" + tmp);
              if ("jfeng001".equals(tmp.getLCObject("follower").getString("username"))) {
                operationSucceed = true;
              }
            }
            latch.countDown();
          }

          @Override
          public void onError(Throwable throwable) {
            throwable.printStackTrace();
            latch.countDown();
          }

          @Override
          public void onComplete() {

          }
        });
      }

      @Override
      public void onError(Throwable throwable) {
        throwable.printStackTrace();
        latch.countDown();
      }

      @Override
      public void onComplete() {
      }
    });

    latch.await();
    assertTrue(operationSucceed);
  }

  public void testUnfollow() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    LCUser logginUser = LCUser.logIn("jfeng001", DEFAULT_PASSWD).blockingFirst();
    logginUser.unfollowInBackground(JFENG_OBJECT_ID).subscribe(new Observer<JSONObject>() {
      @Override
      public void onSubscribe(Disposable disposable) {

      }

      @Override
      public void onNext(JSONObject object) {
        System.out.println("succeed to unfollow. " + object.toString());

        LCUser jfeng = LCUser.logIn("jfeng", DEFAULT_PASSWD).blockingFirst();

        LCQuery query = jfeng.followerQuery();
        query.findInBackground().subscribe(new Observer<List<LCObject>>() {
          @Override
          public void onSubscribe(Disposable disposable) {

          }

          @Override
          public void onNext(List<LCObject> o) {
            System.out.println("onNext");
            operationSucceed = (null == o) || o.size() < 1;
            latch.countDown();
          }

          @Override
          public void onError(Throwable throwable) {
            System.out.println("onError");
            throwable.printStackTrace();
            latch.countDown();
          }

          @Override
          public void onComplete() {
            System.out.println("onComplete");
          }
        });
      }

      @Override
      public void onError(Throwable throwable) {
        throwable.printStackTrace();
        latch.countDown();
      }

      @Override
      public void onComplete() {

      }
    });


    latch.await();
    assertTrue(operationSucceed);
  }

  public void testFollowUserNotLogin() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    LCQuery<? extends LCUser> query = LCUser.getQuery();
    query.findInBackground().subscribe(new Observer<List<? extends LCUser>>() {
      @Override
      public void onSubscribe(Disposable disposable) {

      }

      @Override
      public void onNext(List<? extends LCUser> avUsers) {
        LCUser target = avUsers.get(0);
        target.followInBackground("5bff479067f3560066d00676").subscribe(new Observer<JSONObject>() {
          @Override
          public void onSubscribe(Disposable disposable) {

          }

          @Override
          public void onNext(JSONObject jsonObject) {
            latch.countDown();
          }

          @Override
          public void onError(Throwable throwable) {
            throwable.printStackTrace();
            operationSucceed = true;
            latch.countDown();
          }

          @Override
          public void onComplete() {

          }
        });

      }

      @Override
      public void onError(Throwable throwable) {
        throwable.printStackTrace();
        latch.countDown();
      }

      @Override
      public void onComplete() {

      }
    });
    latch.await();
    assertTrue(operationSucceed);
  }

  public void testFolloweeAndFollowerQuery() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    LCUser.logIn("jfeng", DEFAULT_PASSWD).subscribe(new Observer<LCUser>() {
      public void onSubscribe(Disposable disposable) {
        System.out.println("onSubscribe " + disposable.toString());
      }

      public void onNext(LCUser avUser) {
        avUser.getFollowersAndFolloweesInBackground(new FollowersAndFolloweesCallback() {
          @Override
          public void done(Map avObjects, LCException LCException) {
            operationSucceed = (null != avObjects);
            System.out.println(JSON.toJSONString(avObjects.get("follower")));
            System.out.println(JSON.toJSONString(avObjects.get("followee")));
            if (null != LCException) {
              LCException.printStackTrace();
            }
            latch.countDown();
          }

        });
      }

      public void onError(Throwable throwable) {
        System.out.println("failed to login. cause: " + throwable.getMessage());
        latch.countDown();
      }

      public void onComplete() {
      }
    });
    latch.await();
    assertTrue(operationSucceed);
  }

}
