package com.hp.extracredit;

import android.content.Context;
import android.print.PrintJobInfo;
import android.printservice.PrintJob;

public class JobOptions {
    private static final int DEFAULT_SCALE = 818;
    public int mScale;

    private JobOptions(JobOptions other) {
        mScale = other.mScale;
    }

    private JobOptions(int scale) {
        mScale = scale;
    }

    public static JobOptions extract(Context context, PrintJob job) {
        String scaleString = job.getAdvancedStringOption(context.getString(R.string.scale_image_key));
        int scale = DEFAULT_SCALE;
        try {
            scale = Integer.parseInt(scaleString);
        } catch (NumberFormatException ignore) {
        }
        return new JobOptions(scale);
    }

    public int getScale() {
        return mScale;
    }

    public PrintJobInfo record(Context context, PrintJobInfo jobInfo) {
        PrintJobInfo.Builder builder = new PrintJobInfo.Builder(jobInfo);
        builder.putAdvancedOption(
                context.getString(R.string.scale_image_key),
                Integer.toString(mScale));
        return builder.build();
    }

    @Override
    public String toString() {
        return "JobOptions(" +
                "scale=" + mScale +
                ")";
    }

    public static class Builder {
        JobOptions mPrototype;

        public Builder() {
            mPrototype = new JobOptions(640);
        }

        public Builder(JobOptions other) {
            mPrototype = new JobOptions(other);
        }

        public Builder setScale(int scale) {
            mPrototype.mScale = scale;
            return this;
        }
        public JobOptions build() {
            return new JobOptions(mPrototype);
        }
    }
}
