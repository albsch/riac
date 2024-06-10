package link.server1.ag.concurrency.riac;

import lombok.Data;

@Data
class SingleBenchmark {
    private final String version;
    private final String args;
    private final String A;
    private final String B;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SingleBenchmark that = (SingleBenchmark) o;

        if (A != null ? !A.equals(that.A) : that.A != null) return false;
        return B != null ? B.equals(that.B) : that.B == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (A != null ? A.hashCode() : 0);
        result = 31 * result + (B != null ? B.hashCode() : 0);
        return result;
    }
}
