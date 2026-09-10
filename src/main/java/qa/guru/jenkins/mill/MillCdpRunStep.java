package qa.guru.jenkins.mill;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Pipeline step {@code millCdpRun}: POST a URL on the allocated agent via remoting
 * {@code HttpURLConnection} (no {@code sh}).
 */
public final class MillCdpRunStep extends Step {
  private final String url;

  @DataBoundConstructor
  public MillCdpRunStep(@NonNull String url) {
    this.url = url;
  }

  public String getUrl() {
    return url;
  }

  @Override
  public StepExecution start(StepContext context) {
    return new Execution(context, url);
  }

  public static final class Execution extends SynchronousNonBlockingStepExecution<String> {
    private static final long serialVersionUID = 1L;
    private final String url;

    Execution(StepContext context, String url) {
      super(context);
      this.url = url;
    }

    @Override
    protected String run() throws Exception {
      Launcher launcher = getContext().get(Launcher.class);
      VirtualChannel channel = launcher == null ? null : launcher.getChannel();
      if (channel == null) {
        throw new IllegalStateException("millCdpRun needs an agent");
      }
      String body = channel.call(new Post(url));
      TaskListener listener = getContext().get(TaskListener.class);
      if (listener != null) {
        listener.getLogger().println(body);
      }
      if (!body.contains("\"ok\":true")) {
        throw new AbortException("mill not ok: " + body);
      }
      return body;
    }
  }

  public static final class Post extends MasterToSlaveCallable<String, IOException> {
    private static final long serialVersionUID = 1L;
    private final String url;

    Post(String url) {
      this.url = url;
    }

    @Override
    public String call() throws IOException {
      HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
      try {
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(60000);
        conn.setDoOutput(true);
        conn.setFixedLengthStreamingMode(0);
        conn.getOutputStream().close();
        int code = conn.getResponseCode();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        InputStream raw = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (raw != null) {
          try (InputStream stream = raw) {
            stream.transferTo(buf);
          }
        }
        String text = new String(buf.toByteArray(), StandardCharsets.UTF_8).trim();
        if (code != 200) {
          throw new IOException("HTTP " + code + ": " + text);
        }
        return text;
      } finally {
        conn.disconnect();
      }
    }
  }

  @Extension
  public static final class DescriptorImpl extends StepDescriptor {
    @Override
    public String getFunctionName() {
      return "millCdpRun";
    }

    @NonNull
    @Override
    public String getDisplayName() {
      return "mill CDP POST /run";
    }

    @Override
    public Set<? extends Class<?>> getRequiredContext() {
      return Collections.singleton(Launcher.class);
    }
  }
}
