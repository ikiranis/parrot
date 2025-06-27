package com.parrottunes.service;

import com.parrottunes.entity.QueueItem;
import com.parrottunes.entity.MediaFile;
import com.parrottunes.repository.QueueItemRepository;
import com.parrottunes.repository.MediaFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class QueueService {

    @Autowired
    private QueueItemRepository queueItemRepository;

    @Autowired
    private MediaFileRepository mediaFileRepository;

    public List<QueueItem> getQueue() {
        return queueItemRepository.findAllByOrderByCreatedAtAsc();
    }

    public QueueItem addToQueue(Long fileId) {
        Optional<MediaFile> fileOpt = mediaFileRepository.findById(fileId);
        if (fileOpt.isPresent()) {
            QueueItem queueItem = new QueueItem(fileOpt.get());
            return queueItemRepository.save(queueItem);
        }
        throw new RuntimeException("File not found with id: " + fileId);
    }

    public void removeFromQueue(Long queueItemId) {
        queueItemRepository.deleteById(queueItemId);
    }

    public Optional<QueueItem> getNextInQueue() {
        List<QueueItem> queue = queueItemRepository.findAllByOrderByCreatedAtAsc();
        return queue.isEmpty() ? Optional.empty() : Optional.of(queue.get(0));
    }

    public void clearQueue() {
        queueItemRepository.deleteAll();
    }

    public long getQueueSize() {
        return queueItemRepository.count();
    }
}
