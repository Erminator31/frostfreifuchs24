package com.example.fff.model;


public class JobStatus {
    public enum Status {
        RUNNING,
        DONE,
        ERROR
    }

    private String jobId;
    private Status status;
    private String message; // can store progress, error info, etc.

    public JobStatus(String jobId, Status status, String message) {
        this.jobId = jobId;
        this.status = status;
        this.message = message;
    }

    public String getJobId() {
        return jobId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

